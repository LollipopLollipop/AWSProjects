#!/usr/bin/python

import sys

curTitle = None
#list to hold daily view count of the page being analyzed
i = 0
dailyCountList = [0 for i in range(31)]
#monthly view count of the page being analyzed
monthlyCount = 0
#string to print out the page views for all dates in a single line
pageViewsInfo = ''
#results after parsing the intermediate <key, value> pairs outputted by mapper
dateCount = ''
title = ''

emergeCount = 0

for line in sys.stdin:
	line = line.strip()
	#input parsing
	title, dateCount = line.split('\t')
	date = dateCount[:8]
	count = dateCount[8:]

	try:	
		count = int(count)
		date = int(date)
	except ValueError:
		continue
	#title sorting already taken care of before reducer
	#aggregate page views when analyzing one page
	if (curTitle == title):
		monthlyCount += count
		dailyCountList[date-20140701] += count
	#summarize page view info for last page before jumping to next page 
	else:
		if (monthlyCount > 100000):
			emergeCount += 1
			i = 0
			#print view counts for each day
			for i in range(31):
				pageViewsInfo = pageViewsInfo + (str(20140701+i) + ':' + str(dailyCountList[i])) + '\t'
			
			print '%s\t%s\t%s' % (str(monthlyCount), curTitle, pageViewsInfo)
		#update var content for new page analysis
		monthlyCount = count
		i = 0
		dailyCountList = [0 for i in range(31)]
		dailyCountList[date-20140701] = count
		curTitle = title
		pageViewsInfo = ''

#summarize page view info for the last page analyzed
if (monthlyCount > 100000):
	emergeCount += 1
	i = 0
	for i in range(31):
		pageViewsInfo = pageViewsInfo + (str(20140701+i) + ':' + str(dailyCountList[i])) + '\t'
	print '%s\t%s\t%s' % (str(monthlyCount), curTitle, pageViewsInfo)
#print total number of pages emerged after analysis
print 'Q7: ' + str(emergeCount) + ' lines of output emerged'
