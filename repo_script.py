import subprocess
import os
import shutil
import sys
import time

splitter = "git@github.com:oguzdemir/Test-Splitter.git"
inst_path = "repositories"

repos = [
    'https://github.com/CrawlScript/WebCollector', 'https://github.com/spring-projects/spring-boot',
    'https://github.com/kevinsawicki/http-request', 'https://github.com/dropwizard/dropwizard',
    'https://github.com/TooTallNate/Java-WebSocket', 'https://github.com/apache/struts',
    'https://github.com/thinkaurelius/titan', 'https://github.com/oryxproject/oryx',
    'https://github.com/Activiti/Activiti', 'https://github.com/apache/hadoop',
    'https://github.com/apache/jackrabbit-oak', 'https://github.com/square/retrofit',
    'https://github.com/alibaba/fastjson', 'https://github.com/doanduyhai/achilles', 'https://github.com/apache/flink',
    'https://github.com/wro4j/wro4j', 'https://github.com/wildfly/wildfly', 'https://github.com/knightliao/disconf',
    'https://github.com/addthis/stream-lib', 'https://github.com/looly/hutool', 'https://github.com/jfree/jfreechart',
    'https://github.com/orbit/orbit', 'https://github.com/elasticjob/elastic-job-lite',
    'https://github.com/apache/incubator-dubbo', 'https://github.com/spotify/helios', 'URL',
    'https://github.com/undertow-io/undertow', 'https://github.com/apache/hbase']

dependency_xml = [
"<dependency>", "\t<groupId>od</groupId>", "\t<artifactId>Test-Splitter</artifactId>",
"\t<version>1.0-SNAPSHOT</version>", "\t<scope>system</scope>",
"\t<systemPath>${project.basedir}/../../Test-Splitter-1.0-SNAPSHOT-jar-with-dependencies.jar</systemPath>",
"</dependency>"
]

def construct_xml(indent):
    ind_string = ' ' * indent + '\t'
    s = '\n'
    for elem in dependency_xml:
        s = s + ind_string + elem + '\n'
    return s


def run_something(*args, cwd="./", file_name=None):
    result = subprocess.run(args, stderr=subprocess.PIPE, stdout=subprocess.PIPE, cwd=cwd)

    if file_name is not None:
        with open(file_name, 'w+') as content_file:
            content_file.write(result.stderr.decode("utf-8"))
            content_file.write(result.stdout.decode("utf-8"))
            content_file.close()

    time.sleep(1)


def to_data(out_string):
    data = [0, 0, 0, 0]
    infos = out_string.split(" ")
    data[0] = int(infos[infos.index("run:") + 1].replace(',', ''))
    data[1] = int(infos[infos.index("Failures:") + 1].replace(',', ''))
    data[2] = int(infos[infos.index("Errors:") + 1].replace(',', ''))
    data[3] = float(infos[infos.index("elapsed:") + 1])
    return data


def find_instrumentation_time(file_name):
    with open("./" + file_name, 'r') as fp:
        line = fp.readline()
        while line:
            if "Instrumentation took: " in line:
                temp = line.rstrip("\n").split(" ")
                return temp[temp.index("took:") + 1]
            line = fp.readline()


def file_to_map(file_name):
    res = dict()
    entire_data = [0, 0, 0, 0]

    with open(file_name, 'r') as fp:
        line = fp.readline()
        full_name = 0  # Throws error if it is accessed before assignment
        while line:
            if "Running " in line:
                temp = line.rstrip("\n").split(" ")
                full_name = temp[temp.index("Running") + 1]
            if "Tests run: " in line and ", Time elapsed: " in line:
                data = to_data(line.rstrip("\n"))
                *others, partial_name = full_name.split(".")
                class_name = full_name.replace("." + partial_name, "")
                if class_name in res:
                    res[class_name] = [sum(x) for x in zip(res[class_name], data)]
                else:
                    res[class_name] = data
                entire_data = [sum(x) for x in zip(entire_data, data)]
            line = fp.readline()

    return res, entire_data


def change_pom(file_path):
    index = 0
    with open(file_path, 'r') as content_file:
        content = content_file.read()
        ind = content.find("<dependencies>", index)
        while ind > 0:
            ind2 = ind - 1
            while content[ind2] != '\n':
                ind2 = ind2 - 1

            content = content[:ind + 14] + construct_xml(ind - ind2) + content[ind + 14:]
            ind = content.find("<dependencies>", ind + 15)

    with open(file_path, 'w') as content_file:
        content_file.write(content)
        content_file.close()


def go_single_repo(address):
    first_list = os.listdir(".")
    run_something("git", "clone", address)
    second_list = os.listdir(".")
    repo = (set(second_list) - set(first_list)).pop()
    os.mkdir(repo + "--")
    shutil.move(repo, repo + "--/")
    shutil.move(repo + "--", repo)
    shutil.copytree(repo + "/" + repo, repo + "/" + repo + "_original")
    change_pom(repo + "/" + repo + "/pom.xml")
    shutil.copytree(repo + "/" + repo, repo + "/" + repo + "_splitted")
    os.chdir(repo)
    run_something("java", "-jar", "Test-Splitter-1.0-SNAPSHOT-jar-with-dependencies.jar", "-a",
                  "-p", "./" + repo + "/" + repo, cwd="../", file_name="instrumentation.txt")
    run_something("mvn", "test", cwd="./" + repo + "_original", file_name="original.txt")
    run_something("mvn", "test", cwd="./" + repo, file_name="generation.txt")
    try:
        shutil.copytree("./" + repo + "/snapshots", "./" + repo + "_splitted/snapshots")
    except BaseException:
        pass
    run_something("mvn", "test", cwd="./" + repo + "_splitted", file_name="splitted.txt")

    original, original_data = file_to_map("original.txt")
    generation, generation_data = file_to_map("generation.txt")
    splitted, splitted_data = file_to_map("splitted.txt")

    with open("./module_results.csv", 'w+') as fp:
        fp.write(
            "Module Name, Original Tests, Generated Tests, Errors, Failures, Elapsed Time, Time For Snapshots, Original Elapsed Time\n")
        for d in splitted:
            fp.write("%s,%d,%d,%d,%d,%f,%f,%f\n" % (
            d, original[d][0], splitted[d][0], splitted[d][1], splitted[d][2], splitted[d][3], generation[d][3],
            original[d][3]))

    os.chdir("../")

    original_data.extend(generation_data)
    original_data.extend(splitted_data)
    return original_data


def main():
    # if len(sys.argv) < 2:
    #     print("No address given!")
    #     exit(1)
    # address = sys.argv[1]

    shutil.rmtree(inst_path, ignore_errors=True)
    os.mkdir(inst_path)
    os.chdir(inst_path)
    run_something("git", "clone", splitter)
    shutil.copy("./Test-Splitter/jar/Test-Splitter-1.0-SNAPSHOT-jar-with-dependencies.jar", "./")

    with open("./allRes1.csv", 'w+') as result_file:
        result_file.write(
            "Repo, Original Tests, Generated Tests, Errors, Failures, Elapsed Time, Time For Snapshots, Original Elapsed Time\n")
        for x in range(len(repos)):
            try:
                all_data = go_single_repo(repos[x])
            except Exception:
                continue
            repo_name = repos[x][repos[x].rfind("/") + 1:repos[x].find(".git")]
            result_file.write("%s,%d,%d,%d,%d,%f,%f,%f\n" % (
            repo_name, all_data[0], all_data[8], all_data[9], all_data[10], all_data[3], all_data[7], all_data[11]))


if __name__ == "__main__":
    main()
