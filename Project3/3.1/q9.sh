#!/bin/bash
join -t ',' million_songs_sales_data.csv million_songs_metadata.csv > million_songs_metadata_and_sales.csv
sort -t ',' -k 7 million_songs_metadata_and_sales.csv | python q9.py | sort -t ',' -r -k 3 -n | head -3
