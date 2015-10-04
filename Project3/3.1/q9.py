#!/usr/bin/python

import sys
prev_artist_id=""
prev_artist_name=""
total_sales_count=0

for line in sys.stdin:
	elements = line.split(',')
	artist_id = elements[6]
	artist_name = elements[8]
	sales_count = int(elements[2])
	if(artist_id == prev_artist_id):
		total_sales_count += sales_count
	else:
		if(prev_artist_id != ""):
			print prev_artist_id + ',' + prev_artist_name + ',' + str(total_sales_count) + '\n'
		total_sales_count = sales_count
		prev_artist_id = artist_id
		prev_artist_name = artist_name

print prev_artist_id, + ',' + prev_artist_name + ',' + str(total_sales_count) + '\n'
