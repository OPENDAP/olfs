def simple_form(simple_str):
#
	lexico_str = ''
	while simple_str:
		simple_char = simple_str[0]
		simple_str = simple_str[1:]
		#
		if simple_char.isalnum(): 
			if simple_char.isalpha(): lexico_str = lexico_str + '1'
			else: lexico_str = lexico_str + '0'
		else: lexico_str = lexico_str + '2'
	return lexico_str
#

def special_char_form(simple_str):
#
	lexico_str = ''
	while simple_str:
		simple_char = simple_str[0]
		simple_str = simple_str[1:]
		#
		if simple_char.isalnum(): 
			if simple_char.isalpha(): lexico_str = lexico_str + '1'
			else: lexico_str = lexico_str + '0'
		else: lexico_str = lexico_str + simple_char
	return lexico_str
#

def alpha_char_form(simple_str):
#
	lexico_str = ''
	while simple_str:
		simple_char = simple_str[0]
		simple_str = simple_str[1:]
		#
		if simple_char.isalnum(): 
			if simple_char.isalpha(): lexico_str = lexico_str + simple_char
			else: lexico_str = lexico_str + '0'
		else: lexico_str = lexico_str + simple_char
	return lexico_str
#

def simple_hash_value(simple_str):
#
	lexico_val = 0
	#
	while simple_str:
		simple_char = simple_str[0]
		simple_str = simple_str[1:]
		#
		lexico_val = lexico_val + ord(simple_char)
	#
	return lexico_val
#


def dds_char_form(simple_array):
#
	lexico_val = 0

	for i in range( 1, len(simple_array)-2 ):
		dds_var = simple_array[i]
		#
		while dds_var:
			#x = dds_var.find('[')
			#if x >= 0:
				#dds_var = dds_var[0:x]
				#print dds_var

			simple_char = dds_var[0]
			dds_var = dds_var[1:]
			lexico_val = lexico_val + ord(simple_char)
		#
	#
	return lexico_val
#

def dds_2_char_form(simple_array):
#
	lexico_val = 0

	for i in range( 1, len(simple_array)-2 ):
		dds_var = simple_array[i]
		#
		while dds_var:
			x = dds_var.find('[')
			if x >= 0:
				dds_var = dds_var[0:x]
				#print dds_var

			simple_char = dds_var[0]
			dds_var = dds_var[1:]
			lexico_val = lexico_val + ord(simple_char)
		#
	#
	return lexico_val
#

def is_empty(simple_str):
#
	empty = 1
	
	das = simple_str.split('\n')

	for i in range( 1, len(das)-2 ):

		das_line = das[i].strip()
		#
		if das_line.find('{') >= 0:
			#print das_line
			pass
		elif das_line.find('}') >= 0:
			#print das_line
			pass
		else:
			#print das_line
			empty = 0
			break
	#
	return empty
#

def das_char_form(simple_str):
#
	lexico_val = 0
	
	das = simple_str.split('\n')

	for i in range( 1, len(das)-2 ):

		das_line = das[i].strip()
		#
		if das_line.find('{') >= 0:
			#print das_line
			lexico_val = lexico_val + simple_hash_value(das_line)
		elif das_line.find('}') >= 0:
			#print das_line
			lexico_val = lexico_val + simple_hash_value(das_line)
		else:
			#print das_line
			attributes = das_line.split(' ')

			if attributes[0].lower() == 'string':
				attribute = attributes[0] + ' ' + attributes[1]
				#print attribute
				lexico_val = lexico_val + simple_hash_value(attribute)
			elif attributes[0].lower() == 'float32':
				attribute = attributes[0] + ' ' + attributes[1]
				#print attribute
				lexico_val = lexico_val + simple_hash_value(attribute)
			elif attributes[0].lower() == 'float64':
				attribute = attributes[0] + ' ' + attributes[1]
				#print attribute
				lexico_val = lexico_val + simple_hash_value(attribute)
			elif attributes[0].lower() == 'int16':
				attribute = attributes[0] + ' ' + attributes[1]
				#print attribute
				lexico_val = lexico_val + simple_hash_value(attribute)
			elif attributes[0].lower() == 'uint16':
				attribute = attributes[0] + ' ' + attributes[1]
				#print attribute
				lexico_val = lexico_val + simple_hash_value(attribute)
			elif attributes[0].lower() == 'int32':
				attribute = attributes[0] + ' ' + attributes[1]
				#print attribute
				lexico_val = lexico_val + simple_hash_value(attribute)
			elif attributes[0].lower() == 'uint32':
				attribute = attributes[0] + ' ' + attributes[1]
				#print attribute
				lexico_val = lexico_val + simple_hash_value(attribute)
			elif attributes[0].lower() == 'int8':
				attribute = attributes[0] + ' ' + attributes[1]
				#print attribute
				lexico_val = lexico_val + simple_hash_value(attribute)
			elif attributes[0].lower() == 'uint8':
				attribute = attributes[0] + ' ' + attributes[1]
				#print attribute
				lexico_val = lexico_val + simple_hash_value(attribute)
			elif attributes[0].lower() == 'byte':
				attribute = attributes[0] + ' ' + attributes[1]
				#print attribute
				lexico_val = lexico_val + simple_hash_value(attribute)
			#else:
				#print 'continuation ', attributes[0]			
	#
	return lexico_val
#

def number_of_name_facets(simple_str):
#
	num_facets = simple_str.split('.')
	return len(num_facets)
#

def number_of_path_facets(simple_str):
#
	num_facets = simple_str.split('/')
	return len(num_facets)
#
def facets(simple_str):
#
	return simple_str.split('.')


def lex_facet_stats(log_file):
#
	import MySQLdb
#

	db = MySQLdb.connect("localhost","root","","dapadl")
	cursor = db.cursor()

	db_cache = MySQLdb.connect("localhost","root","","dapadl_cache")
	cursor_cache = db_cache.cursor()

	sql = """SELECT id,server_id,schar_facet FROM datasources WHERE id <= 1408"""
	#sql = """SELECT id,server_id,schar_facet FROM datasources WHERE id <= 998"""
	cursor.execute(sql)

	name_cluster = {}

	for i in range(1408):
		null_dds = 0
		null_das = 0
		#
        	record = cursor.fetchone()
		id = record[0]
	        server = record[1]
	        facet = record[2]

		dds_cache = "SELECT cache_dds FROM datasource_cache WHERE has_dds = 1 and datasource_id = %d" % (id)
		#dds_cache = "SELECT cache_dds FROM datasource_cache WHERE has_dds = 1 and datasource_id = 998"
		cursor_cache.execute(dds_cache)

		ds_record = cursor_cache.fetchone()
		if ds_record:
			dds_array = ds_record[0].split('\n')
			dds = dds_char_form(dds_array)
		else:
			dds = 0
			null_dds = 1

		#print ds_record
		#print dds, null_dds

		das_cache = "SELECT cache_das FROM datasource_cache WHERE has_das = 1 and datasource_id = %d" % (id)
		#das_cache = "SELECT cache_das FROM datasource_cache WHERE has_das = 1 and datasource_id = 998"
		cursor_cache.execute(das_cache)

		ds_record = cursor_cache.fetchone()
		if ds_record:
			#das_array = ds_record[0].split('\n')
			das = das_char_form(ds_record[0])
		else:
			das = 0
			null_das = 1

		#print ds_record
		#print das, null_das

		if name_cluster.has_key(facet):
			x = name_cluster[facet][1]
			if x.has_key(server):
				x[server] = x[server] + 1
			else:
				x[server] = 1
			name_cluster[facet][0] = name_cluster[facet][0] + 1
			name_cluster[facet][1] = x
			#
			x = name_cluster[facet][2]
			if x.has_key(dds):
				x[dds] = x[dds] + 1
			else:
				x[dds] = 1
			name_cluster[facet][2] = x
			#
			x = name_cluster[facet][4]
			if x.has_key(das):
				x[das] = x[das] + 1
			else:
				x[das] = 1
			name_cluster[facet][4] = x
		else:
			name_cluster[facet] = [ 1, { server : 1 }, { dds : 1 }, null_dds, { das : 1 }, null_das ]

	log = open(log_file, 'a')

	keys = name_cluster.keys()
	for i in range(len(keys)):
		print >> log, i, '\t', name_cluster[keys[i]][0], '\t', len(name_cluster[keys[i]][1]), '\t', len(name_cluster[keys[i]][2]), '\t', name_cluster[keys[i]][3], '\t', len(name_cluster[keys[i]][4]), '\t', name_cluster[keys[i]][5] 
		#print >> log, i, '\t', name_cluster[keys[i]][2]
#

def alpha_facet_stats(log_file, log_file2):
#
	import MySQLdb
#

	db = MySQLdb.connect("localhost","root","","dapadl")
	cursor = db.cursor()

	db_cache = MySQLdb.connect("localhost","root","","dapadl_cache")
	cursor_cache = db_cache.cursor()

	#sql = """SELECT id,server_id,achar_facet FROM datasources WHERE achar_facet = 'i0_av_ott_0000000000000_v00.cdf' and id < 1408996"""
	#sql = """SELECT id,server_id,achar_facet FROM datasources WHERE id <= 1408996"""
	sql = """SELECT id,server_id,achar_facet,dds_facet,dds_facet_2,das_facet,def_das FROM datasources WHERE id <= 1408996"""
	num_records = cursor.execute(sql)

	name_cluster = {}

	for i in range(num_records):
		null_dds = 0
		null_das = 0
		def_das = 0
		#
        	record = cursor.fetchone()
		id = record[0]
	        server = record[1]
	        facet = record[2]
	        dds = record[3]
	        dds_2 = record[4]
	        das = record[5]
	        def_das = record[6]

		#dds_cache = "SELECT cache_dds FROM datasource_cache WHERE has_dds = 1 and datasource_id = %d" % (id)
		#dds_cache = "SELECT cache_dds FROM datasource_cache WHERE has_dds = 1 and datasource_id = 1319"
		#cursor_cache.execute(dds_cache)

		#ds_record = cursor_cache.fetchone()
		#if ds_record:
			#dds_array = ds_record[0].split('\n')
			#dds = dds_char_form(dds_array)
		#else:
			#dds = 0
			#null_dds = 1

		#print ds_record
		#print dds, null_dds

		#das_cache = "SELECT cache_das FROM datasource_cache WHERE has_das = 1 and datasource_id = %d" % (id)
		#das_cache = "SELECT cache_das FROM datasource_cache WHERE has_das = 1 and datasource_id = 1319"
		#cursor_cache.execute(das_cache)

		#ds_record = cursor_cache.fetchone()
		#if ds_record:
			#das_array = ds_record[0].split('\n')
			#das = das_char_form(ds_record[0])
			#def_das = is_empty(ds_record[0])
		#else:
			#das = 0
			#null_das = 1
			#def_das = 0

		#print ds_record
		#print das, null_das

		if name_cluster.has_key(facet):
			x = name_cluster[facet][1]
			if x.has_key(server):
				x[server] = x[server] + 1
			else:
				x[server] = 1
			name_cluster[facet][0] = name_cluster[facet][0] + 1
			name_cluster[facet][1] = x
			#
			x = name_cluster[facet][2]
			if x.has_key(dds):
				x[dds] = x[dds] + 1
				if dds == 0:
					name_cluster[facet][3] = name_cluster[facet][3] + 1
			else:
				x[dds] = 1
				if dds == 0:
					name_cluster[facet][3] = 1

			name_cluster[facet][2] = x

			#
			x = name_cluster[facet][4]
			if x.has_key(dds_2):
				x[dds_2] = x[dds_2] + 1
			else:
				x[dds_2] = 1

			name_cluster[facet][4] = x

			#
			x = name_cluster[facet][5]
			if x.has_key(das):
				x[das] = x[das] + 1
			else:
				x[das] = 1
				if def_das == 1:
					name_cluster[facet][6] = name_cluster[facet][6] + 1

			name_cluster[facet][5] = x
		else:
			if dds == 0:
				null_dds = 1

			name_cluster[facet] = [ 1, { server : 1 }, { dds : 1 }, null_dds, { dds_2 : 1 }, { das : 1 }, def_das, facet ]

	log = open(log_file, 'a')
	log2 = open(log_file2, 'a')

	keys = name_cluster.keys()
	for i in range(len(keys)):
		print >> log, i, '\t', name_cluster[keys[i]][0], '\t', len(name_cluster[keys[i]][1]), '\t', len(name_cluster[keys[i]][2]), '\t', name_cluster[keys[i]][3], '\t', len(name_cluster[keys[i]][4]), '\t', len(name_cluster[keys[i]][5]), '\t', name_cluster[keys[i]][6] 
		print >> log2, i, '\t', name_cluster[keys[i]][0], '\t', len(name_cluster[keys[i]][1]), '\t', len(name_cluster[keys[i]][2]), '\t', name_cluster[keys[i]][3], '\t', len(name_cluster[keys[i]][4]), '\t', len(name_cluster[keys[i]][5]), '\t', name_cluster[keys[i]][6], '\t', name_cluster[keys[i]][7] 
		#print >> log, i, '\t', name_cluster[keys[i]][2]
#	

def get_dds_hash(record_number):
#
	import MySQLdb
#

	db_cache = MySQLdb.connect("localhost","root","","dapadl_cache")
	cursor_cache = db_cache.cursor()

	sql_cache = "SELECT cache_dds FROM datasource_cache WHERE has_dds = 1 and datasource_id = %d" % (record_number)
	cursor_cache.execute(sql_cache)

	null_dds = 0
	ds_record = cursor_cache.fetchone()
	if ds_record:
		dds_array = ds_record[0].split('\n')
		dds = dds_char_form(dds_array)
	else:
		dds = 0
		null_dds = 1
		
	print ds_record
	print dds, null_dds
#

def dds_facet_stats(log_file, log_file2):
#
	import MySQLdb
#

	db = MySQLdb.connect("localhost","root","","dapadl")
	cursor = db.cursor()

	db_cache = MySQLdb.connect("localhost","root","","dapadl_cache")
	cursor_cache = db_cache.cursor()

	#sql = """SELECT id,server_id,achar_facet FROM datasources WHERE achar_facet = 'i0_av_ott_0000000000000_v00.cdf' and id < 1408996"""
	#sql = """SELECT id,server_id,achar_facet FROM datasources WHERE id <= 1408996"""
	sql = """SELECT id,server_id,achar_facet,dds_facet_2 FROM datasources WHERE id <= 1408996"""
	num_records = cursor.execute(sql)

	name_cluster = {}

	for i in range(num_records):
		null_dds = 0
		null_das = 0
		def_das = 0
		#
        	record = cursor.fetchone()
		id = record[0]
	        server = record[1]
	        achar = record[2]
	        facet = record[3]
	        #dds_2 = record[4]
	        #das = record[5]
	        #def_das = record[6]

		#dds_cache = "SELECT cache_dds FROM datasource_cache WHERE has_dds = 1 and datasource_id = %d" % (id)
		#dds_cache = "SELECT cache_dds FROM datasource_cache WHERE has_dds = 1 and datasource_id = 1319"
		#cursor_cache.execute(dds_cache)

		#ds_record = cursor_cache.fetchone()
		#if ds_record:
			#dds_array = ds_record[0].split('\n')
			#dds = dds_char_form(dds_array)
		#else:
			#dds = 0
			#null_dds = 1

		#print ds_record
		#print dds, null_dds

		#das_cache = "SELECT cache_das FROM datasource_cache WHERE has_das = 1 and datasource_id = %d" % (id)
		#das_cache = "SELECT cache_das FROM datasource_cache WHERE has_das = 1 and datasource_id = 1319"
		#cursor_cache.execute(das_cache)

		#ds_record = cursor_cache.fetchone()
		#if ds_record:
			#das_array = ds_record[0].split('\n')
			#das = das_char_form(ds_record[0])
			#def_das = is_empty(ds_record[0])
		#else:
			#das = 0
			#null_das = 1
			#def_das = 0

		#print ds_record
		#print das, null_das

		if name_cluster.has_key(facet):
			x = name_cluster[facet][1]
			if x.has_key(server):
				x[server] = x[server] + 1
			else:
				x[server] = 1
			name_cluster[facet][0] = name_cluster[facet][0] + 1
			name_cluster[facet][1] = x
			#
			x = name_cluster[facet][2]
			if x.has_key(achar):
				x[achar] = x[achar] + 1
				if facet == 0:
					name_cluster[facet][3] = name_cluster[facet][3] + 1
			else:
				x[facet] = 1
				if facet == 0:
					name_cluster[facet][3] = 1

			name_cluster[facet][2] = x

			#
			#x = name_cluster[facet][4]
			#if x.has_key(dds_2):
			#	x[dds_2] = x[dds_2] + 1
			#else:
			#	x[dds_2] = 1

			#name_cluster[facet][4] = x

			#
			#x = name_cluster[facet][5]
			#if x.has_key(das):
			#	x[das] = x[das] + 1
			#else:
			#	x[das] = 1
			#	if def_das == 1:
			#		name_cluster[facet][6] = name_cluster[facet][6] + 1

			#name_cluster[facet][5] = x
		else:
			if facet == 0:
				null_dds = 1

			name_cluster[facet] = [ 1, { server : 1 }, { achar : 1 }, null_dds ]

	log = open(log_file, 'a')
	log2 = open(log_file2, 'a')

	keys = name_cluster.keys()
	for i in range(len(keys)):
		print >> log, i, '\t', name_cluster[keys[i]][0], '\t', len(name_cluster[keys[i]][1]), '\t', len(name_cluster[keys[i]][2]), '\t', name_cluster[keys[i]][3]
		print >> log2, i, '\t', name_cluster[keys[i]][0], '\t', len(name_cluster[keys[i]][1]), '\t', len(name_cluster[keys[i]][2]), '\t', name_cluster[keys[i]][3]
		#print >> log, i, '\t', name_cluster[keys[i]][2]
#	

def facet_stats(log_file):
#
	import MySQLdb
#

	db = MySQLdb.connect("localhost","root","","dapadl")
	cursor = db.cursor()

	sql = """SELECT id,dds_idx,das_idx,achar_idx,schar_idx,dds2_idx,server_id FROM datasources WHERE id <= 1408996"""
	num_records = cursor.execute(sql)

	log = open(log_file, 'a')
	#log2 = open(log_file2, 'a')

	for i in range(num_records):
        	record = cursor.fetchone()
		id = record[0]
	        dds = record[1]
	        das = record[2]
	        achar = record[3]
	        schar = record[4]
	        dds2 = record[5]
	        server = record[6]


		#print >> log, i, '\t', id, '\t', dds, '\t', das, '\t', achar, '\t', schar, '\t', dds2, '\t', server 
		print >> log, id, '\t', dds, '\t', das, '\t', achar, '\t', schar, '\t', dds2, '\t', server
		#print >> log, i, '\t', name_cluster[keys[i]][2]
#	

def get_achar_facet_stats(log_file, log_file2):
#
	import MySQLdb
#

	db = MySQLdb.connect("localhost","root","","dapadl")
	cursor = db.cursor()

	sql = """SELECT id,dds_idx,achar_idx,achar_facet FROM datasources WHERE dds_idx = 7334 and id <= 1408996"""
	num_records = cursor.execute(sql)

	log = open(log_file, 'a')
	log2 = open(log_file2, 'a')

	name_cluster = {}

	for i in range(num_records):
        	record = cursor.fetchone()
		id = record[0]
	        dds = record[1]
	        facet = record[2]
	        achar_facet = record[3]
		print id, facet, facet, achar_facet

		if name_cluster.has_key(facet):
			name_cluster[facet][2] = name_cluster[facet][2] + 1
		else:
			name_cluster[facet] = [ facet, achar_facet, 1 ]

	keys = name_cluster.keys()
	print len(keys)
	for i in range(len(keys)):
		print i, '\t', name_cluster[keys[i]][0], '\t', name_cluster[keys[i]][1], '\t', name_cluster[keys[i]][2]
		print >> log2, i, '\t', name_cluster[keys[i]][0], '\t', name_cluster[keys[i]][1], '\t', name_cluster[keys[i]][2]
		#print >> log, i, '\t', name_cluster[keys[i]][2]
#	

def process_ip_metrics(hyrax_log, metrics_log)
#
	logF = open(hyrax_log,'r')
	metF = open(metrics_log,'w')

	ip_cluster = {}

	while True:
		line = logF.readline()
		if not line: break
		log_entry = line.split(' ')
		ip_addr = log_entry[0]
		if ip_cluster.has_key(ip_addr):
			ip_cluster[ip_addr][0] += 1
		else:
			ip_cluster[ip_addr] = 1

	keys = ip_cluster.keys()

	for i in len(keys):
		print i, ip_cluster[keys[i]], keys[i]

		
