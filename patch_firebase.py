import re

with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

if 'firebaseBom' not in content:
    content = content.replace('[versions]', '[versions]\nfirebaseBom = "33.1.2"')
    content = content.replace('[libraries]', '[libraries]\nfirebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }\nfirebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }\nfirebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }')
    
    with open('gradle/libs.versions.toml', 'w') as f:
        f.write(content)

with open('app/build.gradle.kts', 'r') as f:
    app_build = f.read()

if 'firebase' not in app_build:
    deps = """
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
"""
    app_build = app_build.replace('dependencies {', 'dependencies {' + deps)
    with open('app/build.gradle.kts', 'w') as f:
        f.write(app_build)
