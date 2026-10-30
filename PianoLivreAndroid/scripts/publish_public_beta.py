#!/usr/bin/env python3
"""Trusted-main prerelease only, after both checks on the exact merge commit."""
import hashlib,json,os,pathlib,subprocess,time

def gh(*args):
    return subprocess.run(['gh',*args],check=True,text=True,stdout=subprocess.PIPE).stdout
repo=os.environ['GITHUB_REPOSITORY'];sha=os.environ['GITHUB_SHA'];before=os.environ['BEFORE_SHA']
changed=subprocess.run(['git','diff','--quiet',before,sha,'--','PianoLivreAndroid/gradle.properties']).returncode
if changed not in (0,1):raise RuntimeError('Cannot compare version commit')
properties=dict(line.split('=',1) for line in pathlib.Path('PianoLivreAndroid/gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
version=properties['pianolivre.beta.versionName'];code=properties['pianolivre.beta.versionCode'];tag='v'+version
if not version.endswith('-open-beta'):raise RuntimeError('Automatic publication accepts open-beta versions only')
# Idempotent recovery also supports a later main commit when this version's
# publication was blocked by a failed check. Existing assets are never replaced.
existing=subprocess.run(['gh','release','view',tag,'--repo',repo,'--json','targetCommitish'],text=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
if existing.returncode==0:
    if json.loads(existing.stdout)['targetCommitish']==sha:
        print('This commit already has a prerelease; assets preserved');raise SystemExit(0)
    if changed==0:
        print('This version is already published; existing release preserved');raise SystemExit(0)
    raise RuntimeError('Version already published from another commit; increment version')
# The full Android suite includes two baseline builds and visual/MIDI journeys.
# Allow up to 40 minutes, within the publication job's 45-minute limit.
for attempt in range(120):
    runs=json.loads(gh('api',f'repos/{repo}/commits/{sha}/check-runs?per_page=100'))['check_runs']
    selected=[next((r for r in runs if r['name']==name),None) for name in ['build','emulator']]
    if all(r and r['status']=='completed' and r['conclusion']=='success' for r in selected):break
    if any(r and r['status']=='completed' and r['conclusion']!='success' for r in selected):raise RuntimeError('Required check failed; prerelease blocked')
    print('Waiting for build/emulator on this exact commit',flush=True);time.sleep(20)
else:raise RuntimeError('Required checks not completed; prerelease blocked')
dist=pathlib.Path('dist');apk=dist/'app/build/outputs/apk/release/app-release.apk';signature=dist/'build/public-signature.txt';metadata=dist/'build/public-apk-metadata.txt'
if '7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290' not in signature.read_text():raise RuntimeError('Permanent certificate missing')
if f"versionCode='{code}' versionName='{version}'" not in metadata.read_text():raise RuntimeError('APK metadata mismatch')
if 'application-debuggable' in metadata.read_text():raise RuntimeError('Distribution APK must not be debuggable')
output=dist/f'PianoLivre-{version}.apk';output.write_bytes(apk.read_bytes())
digest=hashlib.sha256(output.read_bytes()).hexdigest()
checksum=dist/'SHA256SUMS';checksum.write_text(digest+'  '+output.name+'\n')
notes=dist/'notes.md';notes.write_text(f'''Beta pública {version} (versionCode {code}). Commit: {sha}.

APK offline do pacote br.org.pianolivre.opensource, com depuração desativada e assinatura permanente. Build e emulador deste commit aprovados; isso não representa teste físico.

MIDI importado gera notação aproximada, com escolha explícita de mão por trilha e original preservado. Recursos MusicXML incompatíveis são informados. Faça backup antes de atualizar; a edição histórica não é substituída automaticamente.

Realme 7 5G, Lenovo Tab M9 e Yamaha P-45: aceite físico pendente na issue #11. Versão 1.0 ainda não concluída.

Documentação: https://github.com/{repo}/blob/{sha}/docs/10-CONTINUIDADE.md
''',encoding='utf-8')
version_notes=pathlib.Path('docs/releases')/(version+'.md')
if version_notes.is_file():
    notes.write_text(notes.read_text(encoding='utf-8')+'\n'+version_notes.read_text(encoding='utf-8')+f'''

Registro efetivo da publicação:
- Commit integrado: `{sha}`.
- APK SHA-256: `{digest}`.
- Certificado SHA-256: `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`.
- Checks do commit: https://github.com/{repo}/commit/{sha}/checks
- APK: https://github.com/{repo}/releases/download/{tag}/{output.name}
- Consolidação: https://github.com/{repo}/blob/{sha}/docs/73-CONSOLIDACAO-PUBLICACAO-PROMPT-04.md
''',encoding='utf-8')
gh('release','create',tag,'--repo',repo,'--target',sha,'--prerelease','--title','Piano Livre '+version,'--notes-file',str(notes),str(output),str(signature),str(metadata),str(checksum))
print('PUBLIC_BETA_PRERELEASE_PUBLISHED '+tag)
