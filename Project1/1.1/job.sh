#!/bin/bash
s3cmd get s3://wikipediatraf/201407-gz/pagecounts-20140701-000000.gz
gunzip pagecounts-20140701-000000.gz
python seqAnalysis.py

