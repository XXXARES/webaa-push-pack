# Derlenen release APK'dan Firebase push paketini cikarir: dex dosyalari + manifest parcasi (pack.json)
import glob, json, os, re, sys, zipfile
import xml.etree.ElementTree as ET

A = '{http://schemas.android.com/apk/res/android}'
PKG = 'com.webaa.pushpack'

apks = glob.glob('app/build/outputs/apk/release/*.apk')
if not apks:
    sys.exit('Release APK bulunamadi')
apk = apks[0]

cands = glob.glob('app/build/intermediates/merged_manifest*/release/**/AndroidManifest.xml', recursive=True)
mf = next((p for p in cands if 'FirebaseInitProvider' in open(p, encoding='utf-8').read()), None)
if not mf:
    print('Aday manifestler:', cands)
    sys.exit('Birlesik manifest bulunamadi')
print('Manifest:', mf)

gms = None
for root_dir, _, files in os.walk(os.path.expanduser('~/.gradle/caches')):
    if 'values.xml' in files and root_dir.replace('\\', '/').endswith('res/values'):
        m = re.search(r'name="google_play_services_version">(\d+)<', open(os.path.join(root_dir, 'values.xml'), encoding='utf-8', errors='ignore').read())
        if m:
            gms = int(m.group(1))
            break
if not gms:
    gms = 12451000
    print('Uyari: google_play_services_version bulunamadi, 12451000 kullanildi')
print('google_play_services_version:', gms)

unresolved = []

def fix(tag, key, v):
    if key == 'android:authorities' or (tag in ('permission', 'uses-permission') and key == 'android:name'):
        v = v.replace(PKG, '{PKG}')
    if v == '@integer/google_play_services_version' and gms:
        return str(gms)
    if v.startswith('@'):
        unresolved.append((tag, key, v))
    return v

def conv(e):
    attrs = {}
    for k, v in e.attrib.items():
        if k.startswith(A):
            key = 'android:' + k[len(A):]
        elif k.startswith('{'):
            continue
        else:
            key = k
        attrs[key] = fix(e.tag, key, v)
    return {'tag': e.tag, 'attrs': attrs, 'children': [conv(c) for c in e]}

root = ET.parse(mf).getroot()
app = root.find('application')
pack = {
    'version': 1,
    'top': [conv(c) for c in root if c.tag not in ('application', 'uses-sdk')],
    'app': [conv(c) for c in app] if app is not None else [],
    'gmsVersion': gms,
}
print('Cozulmeyen kaynak referanslari:', unresolved)

os.makedirs('dist', exist_ok=True)
out = 'dist/push-pack.zip'
with zipfile.ZipFile(apk) as zin, zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
    dex = sorted(n for n in zin.namelist() if re.fullmatch(r'classes\d*\.dex', n))
    if not dex:
        sys.exit('APK icinde dex yok')
    for n in dex:
        zout.writestr(n, zin.read(n))
    zout.writestr('pack.json', json.dumps(pack, indent=1))
    other = [n for n in zin.namelist() if n not in dex and not n.startswith('META-INF/') and n not in ('AndroidManifest.xml', 'resources.arsc') and not n.startswith('res/')]
    print('Diger APK dosyalari:', other)
print('Paket:', out, os.path.getsize(out), 'bayt; dex:', dex)
