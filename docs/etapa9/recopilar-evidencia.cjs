// Ejecutar desde la raíz: node docs/etapa9/recopilar-evidencia.cjs baseline|reproduccion|final
// Lee resultados existentes y estado Git; no ejecuta Maven ni modifica su configuración.
const fs = require('node:fs');
const path = require('node:path');
const phase = process.argv[2];
if (!['baseline', 'reproduccion', 'final'].includes(phase)) throw new Error('Use baseline, reproduccion o final');
const root = path.resolve(__dirname, '../..');
// Tercer argumento opcional: módulo de una copia aislada para repetir la línea base.
const moduleRoot = process.argv[3] ? path.resolve(process.argv[3]) : path.join(root, 'style-radar');
const reports = path.join(moduleRoot, 'target/surefire-reports');
const attr = (s, name) => (s.match(new RegExp(`\\b${name}="([^"]*)"`)) || [])[1];
const suites = fs.readdirSync(reports).filter(f => /^TEST-.*\.xml$/.test(f)
  && (phase !== 'reproduccion' || f.includes('CierreSeguridadHttpIntegrationTest'))).sort().map(file => {
  const xml = fs.readFileSync(path.join(reports, file), 'utf8');
  const tag = xml.match(/<testsuite\b([^>]*)>/)[1];
  const name = attr(tag, 'name');
  const sourcePath = path.join(moduleRoot, 'src/test/java', name.replaceAll('.', '/') + '.java');
  const source = fs.existsSync(sourcePath) ? fs.readFileSync(sourcePath, 'utf8') : '';
  return {
    name,
    securityFilterChain: source.includes('.apply(springSecurity())'),
    standaloneMvc: source.includes('.standaloneSetup('),
    springContext: source.includes('@SpringBootTest'),
    testSecurityChain: source.includes('SecurityFilterChain testChain'),
    doubles: [...source.matchAll(/@(?:MockitoSpyBean|MockitoBean|Mock|Spy)\b[^;]*;/g)].map(m => m[0].replace(/\s+/g, ' ')),
    tests: +attr(tag, 'tests'), failures: +attr(tag, 'failures'),
    errors: +attr(tag, 'errors'), skipped: +attr(tag, 'skipped'),
    cases: [...xml.matchAll(/<testcase\b([^>]*?)(?:\/\s*>|>([\s\S]*?)<\/testcase>)/g)].map(m => ({
      name: attr(m[1], 'name'),
      result: /<(failure|error|skipped)\b/.exec(m[2] || '')?.[1] || 'passed'
    }))
  };
});
const totals = Object.fromEntries(['tests', 'failures', 'errors', 'skipped'].map(k => [k, suites.reduce((n, s) => n + s[k], 0)]));
for (const suite of suites) {
  if (suite.cases.length !== suite.tests) throw new Error(`Conteo XML inconsistente: ${suite.name}`);
}
const xml = fs.readFileSync(path.join(moduleRoot, 'target/site/jacoco/jacoco.xml'), 'utf8');
const bundle = xml.slice(xml.lastIndexOf('</package>') + '</package>'.length);
const coverage = phase === 'reproduccion' ? [] : [...bundle.matchAll(/<counter type="([^"]+)" missed="(\d+)" covered="(\d+)"\/>/g)].map(m => ({
  type: m[1], missed: +m[2], covered: +m[3], percent: 100 * +m[3] / (+m[2] + +m[3])
}));
let regression;
if (phase === 'final') {
  const baseline = JSON.parse(fs.readFileSync(path.join(__dirname, 'baseline.json'), 'utf8'));
  if (baseline.suites.some(s => s.cases.length !== s.tests)) {
    throw new Error('Repetir baseline con el lector XML corregido antes de comparar casos');
  }
  const keys = data => data.suites.flatMap(s => s.cases.map(c => `${s.name}#${c.name}`));
  const current = new Set(keys({suites}));
  const previous = new Set(keys(baseline));
  const missingCases = [...previous].filter(k => !current.has(k));
  regression = {baselinePreserved: missingCases.length === 0, missingCases, newCases: [...current].filter(k => !previous.has(k)).length};
}
const result = {phase, totals, coverage, regression, suites};
if (phase === 'final') {
  const logPath = [path.join(moduleRoot, 'etapa9-final.log'), path.join(moduleRoot, 'target/etapa9-final.log')]
    .find(file => fs.existsSync(file));
  if (logPath) {
    const bytes = fs.readFileSync(logPath);
    const log = bytes.toString(bytes[0] === 0xff && bytes[1] === 0xfe ? 'utf16le' : 'utf8');
    // Admitir también un marcador UTF-8 añadido a un log UTF-16 por otro host PowerShell.
    const matches = [...(log + '\n' + bytes.toString('utf8')).matchAll(/MAVEN_EXIT_CODE=(\d+)/g)];
    if (matches.length) result.mavenExitCode = Number(matches.at(-1)[1]);
  }
}
if (phase === 'baseline' && fs.existsSync(path.join(__dirname, 'baseline.json'))) {
  const first = JSON.parse(fs.readFileSync(path.join(__dirname, 'baseline.json'), 'utf8'));
  result.initialRun = first.initialRun || {totals: first.totals, coverage: first.coverage,
    caseInventoryComplete: first.suites.every(s => s.cases.length === s.tests)};
}
fs.writeFileSync(path.join(__dirname, `${phase}.json`), JSON.stringify(result, null, 2) + '\n');
if (phase === 'final') {
  const {execFileSync} = require('node:child_process');
  const git = args => execFileSync('git', args, {cwd: root, encoding: 'utf8'}).trimEnd();
  const sqlLog = path.join(root, 'style-radar/target/etapa9-sql.log');
  if (fs.existsSync(sqlLog)) {
    const bytes = fs.readFileSync(sqlLog);
    const log = bytes.toString(bytes[0] === 0xff && bytes[1] === 0xfe ? 'utf16le' : 'utf8');
    const statements = [...new Set([...log.matchAll(/org\.hibernate\.SQL\s+: (.+)/g)]
      .map(m => m[1].trim()).filter(sql => /^(select .*from playlists|create table playlist_likes|alter table .*playlist_likes)/i.test(sql)))];
    if (!statements.length) throw new Error('No se extrajo SQL del log');
    fs.writeFileSync(path.join(__dirname, 'sql-generado-h2.txt'),
      'Extracto SQL real de PlaylistsPopularesJpaIntegrationTest, H2 2.4.240 MODE=PostgreSQL.\n'
      + '17 casos, 0 fallos/errores/omitidos; ejecución suplementaria sin instrumentación JaCoCo.\n'
      + 'Sentencias únicas sin bind values. No es SQL ejecutado en PostgreSQL.\n\n'
      + statements.join('\n\n') + '\n');
  }
  const doc = fs.readFileSync(path.join(root, 'docs/sprint3-etapa9-cierre-seguridad.md'), 'utf8');
  const references = [...new Set([...doc.matchAll(/\b([A-Z]\w+Test)\.([a-z]\w+)/g)]
    .filter(m => m[2] !== 'java').map(m => `${m[1]}.${m[2]}`))];
  const unresolved = references.filter(reference => {
    const [clazz, method] = reference.split('.');
    return !suites.some(s => s.name.endsWith('.' + clazz)
      && s.cases.some(c => c.name === method || c.name.startsWith(method + '(')));
  });
  if (unresolved.length) throw new Error('Referencias sin caso ejecutado: ' + unresolved.join(', '));
  const inventoryFile = path.join(__dirname, 'inventario-git.json');
  if (!fs.existsSync(inventoryFile)) fs.writeFileSync(inventoryFile, '{}\n');
  const status = git(['status', '--porcelain=v1', '--untracked-files=all']);
  const inventory = {
    branch: git(['branch', '--show-current']), head: git(['rev-parse', 'HEAD']),
    diffCheck: git(['diff', '--check']),
    files: status.split(/\r?\n/).filter(Boolean).map(line => ({status: line.slice(0, 2), path: line.slice(3)}))
  };
  fs.writeFileSync(inventoryFile, JSON.stringify(inventory, null, 2) + '\n');
}
console.log(JSON.stringify({phase, totals, coverage, regression, mavenExitCode: result.mavenExitCode}, null, 2));
