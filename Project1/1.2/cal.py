inFile = open('aggreOutput')

maxCount = 0
lineCount = 0

for line in inFile:
	if line.startswith("Q7"):
		continue
	elements = line.split('\t')
	curCount = int(elements[0])
	curTitle = elements[1]
	if curCount > maxCount:
		maxCount = curCount
		maxCountTitle = curTitle 
	lineCount += 1
	if curTitle == 'Cristiano_Ronaldo':
		print '%s\t%s' % (curTitle, str(curCount))
	if curTitle == 'Neymar':
		print '%s\t%s' % (curTitle, str(curCount))
	if curTitle == 'Arjen_Robben':
		print '%s\t%s' % (curTitle, str(curCount))
	if curTitle == 'Tim_Howard':
		print '%s\t%s' % (curTitle, str(curCount))
	if curTitle == 'Miroslav_Klose':
		print '%s\t%s' % (curTitle, str(curCount))

	if curTitle == 'Ariana_Grande':
		print line
	if curTitle == 'Scarlett_Johansson':
		print line
	if curTitle == 'Dwayne_Johnson':
		print line
	if curTitle == 'Iggy_Azalea':
		print line
	if curTitle == 'Kurt_Russell':
		print line
	if curTitle == 'Google':
		print line
	if curTitle == 'Amazon.com':
		print line
	if curTitle == 'Dawn_of_the_Planet_of_the_Apes':
		print line


print 'page with max view is '
print maxCountTitle
print maxCount
print 'number of total page emerged'
print lineCount 