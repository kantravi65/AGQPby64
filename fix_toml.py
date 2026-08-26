with open('gradle/libs.versions.toml', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.startswith('jcifsNg ='):
        pass
    elif line.startswith('jcifs-ng ='):
        pass
    else:
        new_lines.append(line)

new_lines.insert(18, 'jcifsNg = "2.1.10"\n')

lib_index = new_lines.index('[libraries]\n') + 1
new_lines.insert(lib_index, 'jcifs-ng = { group = "eu.agno3.jcifs", name = "jcifs-ng", version.ref = "jcifsNg" }\n')

with open('gradle/libs.versions.toml', 'w') as f:
    f.writelines(new_lines)

