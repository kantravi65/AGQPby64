import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

permissions_insert = '''
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
'''

if "android.permission.FOREGROUND_SERVICE" not in content:
    content = content.replace('<application', permissions_insert + '\n    <application')

service_insert = '''
        <service
            android:name=".service.WebServerService"
            android:foregroundServiceType="dataSync"
            android:exported="false" />
'''

if ".service.WebServerService" not in content:
    content = content.replace('</application>', service_insert + '\n    </application>')

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
