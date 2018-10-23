import subprocess
import os
import shutil
import sys


splitter = "git@github.com:oguzdemir/Test-Splitter.git"

dependency_xml = [
    "<dependency>", "\t<groupId>od</groupId>", "\t<artifactId>Test-Splitter</artifactId>",
    "\t<version>1.0-SNAPSHOT</version>","\t<scope>system</scope>",
    "\t<systemPath>${project.basedir}/../Test-Splitter-1.0-SNAPSHOT-jar-with-dependencies.jar</systemPath>",
    "</dependency>"
]



def construct_xml(indent):
    ind_string = ' ' * indent + '\t'
    s = '\n'
    for elem in dependency_xml:
        s = s + ind_string + elem + '\n'
    return s


def runSomething(*args, cwd="./"):
     return subprocess.run(args, stderr=subprocess.PIPE, stdout=subprocess.PIPE, cwd=cwd )


def goSingleRepo(address):
    firstList = os.listdir(".")
    runSomething("git", "clone", address)
    secondList = os.listdir(".")
    repo = (set(secondList) - set(firstList)).pop()
    shutil.copytree(repo, repo + "_original")
    changePom(repo + "/pom.xml")
    shutil.copytree(repo, repo + "_splitted")

def changePom(filePath):
    index = 0
    with open(filePath, 'r') as content_file:
        content = content_file.read()
        ind = content.find("<dependencies>", index)
        while ind > 0:
            ind2 = ind -1;
            while content[ind2] != '\n':
                ind2 = ind2 - 1;

            content = content[:ind + 14] + construct_xml(ind - ind2) + content[ind+14:]
            ind = content.find("<dependencies>", ind + 15)

    with open(filePath, 'w') as content_file:
        content_file.write(content)
        content_file.close()

def main():
    if len(sys.argv) < 2:
        print("No address given!")
        exit(1)

    address = sys.argv[1]
    runSomething("git", "clone", splitter)
    shutil.copy("./Test-Splitter/jar/Test-Splitter-1.0-SNAPSHOT-jar-with-dependencies.jar", "./")
    goSingleRepo(address)


if __name__ == "__main__":
    main()