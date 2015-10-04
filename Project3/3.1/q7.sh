#!/bin/bash
if [ $# -eq 4 ]; then
	name="$1 $2"
	startyear=$3
	endyear=$4
elif [ $# -eq 3 ]; then
	name=$1
	startyear=$2
	endyear=$3
fi
python q7.py $name $startyear $endyear
