#!/usr/bin/python

import sys
import os

#english page filter 
def eng_filter(file):
	for line in file:
		if(line.startswith("en ")):
			yield line

#non-special page filter for english pages 
def nSpc_filter(file):
	for line in file:
		if(((" Media:") not in line) and ((" Special:") not in line) 
			and ((" Talk:") not in line) and ((" User:") not in line) 
			and ((" User_talk:") not in line) and ((" Project:") not in line)
			and ((" Project_talk:") not in line) and ((" File:") not in line)
			and ((" File_talk:") not in line) and ((" MediaWiki:") not in line)
			and ((" MediaWiki_talk:") not in line) and ((" Template:") not in line)
			and ((" Template_talk:") not in line) and ((" Help:") not in line)
			and ((" Help_talk:") not in line) and ((" Category:") not in line)
			and ((" Category_talk:") not in line) and ((" Portal:") not in line)
			and ((" Wikipedia:") not in line) and ((" Wikipedia_talk:") not in line)):
			yield line

#filter to filter out all articles that start with lowercase English characters
def nLowerCase_filter(file):
	for line in file:
		if((line[3] != "a") and (line[3] != "b") and (line[3] != "c")
			and (line[3] != "d") and (line[3] != "e") and (line[3] != "f")
			and (line[3] != "g") and (line[3] != "h") and (line[3] != "i")
			and (line[3] != "j") and (line[3] != "l") and (line[3] != "m")
			and (line[3] != "n") and (line[3] != "o") and (line[3] != "p")
			and (line[3] != "q") and (line[3] != "r") and (line[3] != "s")
			and (line[3] != "t") and (line[3] != "u") and (line[3] != "v")
			and (line[3] != "w") and (line[3] != "x") and (line[3] != "y") 
			and (line[3] != "z") and (line[3] != "k")):
			yield line
	
#filter to filter out image files 
def nImage_filer(file):
	for line in file:
		if(((".jpg ") not in line) and ((".gif ") not in line) 
			and ((".png ") not in line) and ((".JPG ") not in line)
			and ((".GIF ") not in line) and ((".PNG ") not in line)
			and ((".txt ") not in line) and ((".ico ") not in line)):
			yield line

#filter to exclude boilerplate articles
def nBoilerplate_filter(file):
	for line in file:
		if(((" 404_error/ ") not in line) and ((" Main_Page ") not in line)
			and ((" Hypertext_Transfer_Protocol ") not in line) 
			and ((" Favicon.ico ") not in line)
			and ((" Search ") not in line)):
			yield line
#get the input filename to determine the date being analyzed
inFile = os.environ['mapreduce_map_input_file']
curDate = inFile.split('-')[2]

engList = eng_filter(sys.stdin)

nSpcList = nSpc_filter(engList)

nLowerCaseList = nLowerCase_filter(nSpcList)

nImageList = nImage_filer(nLowerCaseList)

nBoilerplateList = nBoilerplate_filter(nImageList)

for line in nBoilerplateList:
	elements = line.split()
	print '%s\t%s' %(elements[1], curDate+elements[2])


