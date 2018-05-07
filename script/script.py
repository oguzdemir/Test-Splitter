import shlex, subprocess
import csv

SHA_FILE_OUT = "17759f38fb99365cd755deb861e6af95595e5efd"
SHA_STD_OUT = "a38c269c5858377a74c71ef20de9b14f638e8b38"
SHA_NO_OUT = "22535b443415545b5336d38e4664e585b7f007bc"

POM_AGENT_FILE = "testsplit/pom_agent.xml"
POM_DEFAULT_FILE = "testsplit/pom_default.xml"

PROJECT_PATH = "/Users/od/Activiti"
AGENT_PATH = "/Users/od/Test-Splitter"
MAVEN_COMMAND = "mvn test"
# MAVEN_COMMAND = "mvn test -Dtest=org.activiti.editor.language.xml.MessageFlowConverterTest"


def killjava():
    subprocess.call(["killall", "java"])


def initial_run():
    killjava()
    subprocess.check_call(["cp", POM_DEFAULT_FILE , "pom.xml"])
    p = subprocess.Popen(MAVEN_COMMAND, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    for line in p.stdout.readlines():
        pass

    if p.stderr is not None:
        for line in p.stderr.readlines():
            pass

    print ("Initial run has been completed.")


def run_test(d, agent_sha, pom, type):
    killjava()
    print "Running %s and %s" % (agent_sha, pom)

    if agent_sha is not None:
        subprocess.check_call(["git", "checkout", "instrument", "-q"], cwd=AGENT_PATH)
        subprocess.check_call(["git", "checkout", agent_sha, "-q"], cwd=AGENT_PATH)
        subprocess.check_call(["mvn", "clean", "install", "-DskipTests", "-q"], cwd=AGENT_PATH)


    subprocess.call(shlex.split("cp " + pom + " pom.xml"))
    p = subprocess.Popen(MAVEN_COMMAND, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

    key = ""
    for line in p.stdout.readlines():

        if line.startswith("Running "):
            key = line[8:len(line) - 1]

        if line.startswith("Tests run: ") and len(key) > 1:
            index = line.find("Time elapsed")

            if index != -1:
                value = float(line[index + 14:line.find("sec", index) - 1])

                if key not in d:
                    d_val = dict()
                    d_val[type] = value
                    d_val["name"] = key
                    d[key] = d_val
                    key = ""
                else:
                    d[key][type] = value

    retval = p.wait()


def main():
    file_out = dict()

    initial_run()

    run_test(file_out, None, POM_DEFAULT_FILE, "default")
    print "Step 1"
    run_test(file_out, SHA_FILE_OUT, POM_AGENT_FILE, "agent_file")
    print "Step 2"
    run_test(file_out, SHA_STD_OUT, POM_AGENT_FILE, "agent_std")
    print "Step 3"
    run_test(file_out, SHA_NO_OUT, POM_AGENT_FILE, "agent_none")
    print "Step 4"
    with open('csv2.csv', 'wb') as f:
        fields = file_out.values()[0].keys()
        fields.remove('name')
        fields.insert(0, 'name')
        w = csv.DictWriter(f, fields)
        w.writeheader()
        for val in file_out.values():
            w.writerow(val)

if __name__ == "__main__":
    main()
