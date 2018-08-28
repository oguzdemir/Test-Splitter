#!/bin/bash

function measure_times() {
        # Debug the project with various options.
        local existing_name="${1}"; shift
        local generated_name="${1}"; shift

        ( cd "../jfreechart/"
             mvn test -Dtest=$existing_name
        ) 2>&1 1>"output1.txt"

        ( cd "../jfreechart/"
             mvn test -Dtest=$generated_name
        ) 2>&1 1>"output2.txt"

        line1=$(grep -E "Time elapsed:" "output1.txt")
        line2=$(grep -E "Time elapsed:" "output2.txt")
        IFS=', ' read -r -a array1 <<< "$line1"
        IFS=', ' read -r -a array2 <<< "$line2"

        printf "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n" $existing_name $generated_name "${array1[2]}" "${array1[4]}" "${array1[6]}" "${array2[2]}" "${array2[4]}" "${array2[6]}" "${array1[11]}" "${array2[11]}" >> "results.csv"
}

existing="$1"
shift
generated="$1"
readarray existing_classes < $existing
readarray generated_classes < $generated

rm -f "results.csv"
printf "existing_name,generated_name,original_tests,original_failures,original_errors,generated_tests,generated_failures,generated_errors,original_time,generated_time\n" >> "results.csv"

for i in "${!existing_classes[@]}"; do
  measure_times "${existing_classes[$i]}" "${generated_classes[$i]}"
done
