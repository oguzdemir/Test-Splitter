#!/bin/bash

function measure_times() {
        # Debug the project with various options.
		f0=$(grep "Running " "original.txt")
        f1=$(grep "Tests run: " "original.txt")
        f2=$(grep "Tests run: " "generation.txt")
		f3=$(grep "Tests run: " "splitted.txt")
		
		readarray -t names <<< "$f0"
		readarray -t x1 <<< "$f1"
		readarray -t x2 <<< "$f2"
		readarray -t x3 <<< "$f3"
		
		for ((i=0;i<${#x1[@]};++i)); do
			IFS=', ' read -r -a name <<< "${names[i]}"
			IFS=', ' read -r -a array1 <<< "${x1[i]}"
			IFS=', ' read -r -a array2 <<< "${x2[i]}"
			IFS=', ' read -r -a array3 <<< "${x3[i]}"
			#printf "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n" "${name[1]}" "${array1[2]}" "${array1[4]}" "${array1[6]}" "${array2[2]}" "${array2[4]}" "${array2[6]}" "${array3[2]}" "${array3[4]}" "${array3[6]}" "${array1[11]}" "${array2[11]}" "${array3[11]}" >> "results.csv"
			printf "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n" "${name[2]}" "${array1[3]}" "${array1[5]}" "${array1[7]}" "${array2[3]}" "${array2[5]}" "${array2[7]}" "${array3[3]}" "${array3[5]}" "${array3[7]}" "${array1[12]}" "${array2[12]}" "${array3[12]}" >> "results.csv"
		
		done
		
		echo "Hoppala"
}

measure_times
exit 1
RECORDS="snapshots"
RESULTS_FILE="results.csv"
rm -rf "${RESULTS_FILE}"

repo_relative="$1"
shift
test_url_relative="$1"

#cp -r "${repo_relative}_original" "${repo_relative}"
#echo "Original is copied"
#cp -r "${repo_relative}_original" "${repo_relative}_splitted"
#echo "Splitted is copied"

#generation_time="$(TIMEFORMAT='%E,%U,%S'; time ( cd "./Test-Splitter"
#                                                mvn compile exec:java -Dexec.mainClass="com.od.TestSplitter.TestParser" \
#												-Dexec.args="-p ../${repo_relative}/${test_url_relative} -r ${repo_relative} -a"
   #                                            ) 2>&1 1>parser.txt )"
												
#echo "SplitTime," $generation_time
												
(cd "${repo_relative}"
	rm -rf "${RECORDS}"
	mkdir "${RECORDS}"
)

echo "Running original..."
(cd "${repo_relative}_original"
	mvn test 2>&1 1>"../original.txt"
)

echo "Generating records..."
(cd "${repo_relative}"
	mvn test 2>&1 1>"../generation.txt"
)

echo "Copying snapshots..."
cp -r "${repo_relative}/${RECORDS}/" "${repo_relative}_splitted/${RECORDS}/"

echo "Running splitted..."
(cd "${repo_relative}_splitted"
	mvn test 2>&1 1>"../splitted.txt"
)

echo "Test is" $test_url_relative

measure_times