filenames = ['part-00000', 'part-00001', 'part-00002', 'part-00003', 'part-00004',
	'part-00005', 'part-00006', 'part-00007', 'part-00008', 'part-00009', 'part-00010',
	'part-00011', 'part-00012', 'part-00013', 'part-00014', 'part-00015', 'part-00016',
	'part-00017', 'part-00018', 'part-00019', 'part-00020', 'part-00021', 'part-00022',
	'part-00023', 'part-00024', 'part-00025', 'part-00026', 'part-00027', 'part-00028',
	'part-00029', 'part-00030', 'part-00031', 'part-00032', 'part-00033', 'part-00034',
	'part-00035', 'part-00036', 'part-00037', 'part-00038', 'part-00039', 'part-00040',
	'part-00041', 'part-00042', 'part-00043']
with open('aggreOutput', 'w') as outfile:
    for fname in filenames:
        with open(fname) as infile:
            for line in infile:
                outfile.write(line)