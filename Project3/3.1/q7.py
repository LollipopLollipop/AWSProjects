#!/usr/bin/python
import sys
if(len(sys.argv) == 5):
	name = sys.argv[1] + ' ' + sys.argv[2]
	startyear = int(sys.argv[3])
	endyear = int(sys.argv[4])
elif(len(sys.argv) == 4):
	name = sys.argv[1]
	startyear = int(sys.argv[2])
	endyear = int(sys.argv[3])
else:
	print "invalid input"
	sys.exit()
inFile = open("million_songs_metadata.csv")
count = 0
for line in inFile:
	elements = line.split(',')
	artist_name = elements[6]
	year = int(elements[10])
	if((artist_name.lower() == name.lower()) and (year >= startyear) and (year <= endyear)):
		count += 1

print count	
