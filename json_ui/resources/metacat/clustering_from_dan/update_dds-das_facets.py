#
#       Simple Python Main Application
#
import MySQLdb
#
from simple_clustering import dds_char_form
from simple_clustering import dds_2_char_form
from simple_clustering import is_empty
from simple_clustering import das_char_form

db = MySQLdb.connect("localhost","root","","dapadl")
cursor = db.cursor()
cursor_update = db.cursor()
db_cache = MySQLdb.connect("localhost","root","","dapadl_cache")
cursor_cache = db_cache.cursor()

#sql = """SELECT id,server_id,achar_facet FROM datasources WHERE id = 18154"""
sql = """SELECT id,server_id,achar_facet FROM datasources WHERE id <= 1408996"""
num_records = cursor.execute(sql)

for i in range(num_records):

	null_dds = 0
	null_das = 0
	#
       	record = cursor.fetchone()
	id = record[0]
        server = record[1]
        facet = record[2]

	dds_cache = "SELECT cache_dds FROM datasource_cache WHERE has_dds = 1 and datasource_id = %d" % (id)
	cursor_cache.execute(dds_cache)

	ds_record = cursor_cache.fetchone()
	if ds_record:
		dds_array = ds_record[0].split('\n')
		dds = dds_2_char_form(dds_array)
	else:
		dds = 0
		null_dds = 1

	#print ds_record
	print id, dds, null_dds

	das_cache = "SELECT cache_das FROM datasource_cache WHERE has_das = 1 and datasource_id = %d" % (id)
	cursor_cache.execute(das_cache)

	ds_record = cursor_cache.fetchone()
	if ds_record:
		#das_array = ds_record[0].split('\n')
		#das = das_char_form(ds_record[0])
		def_das = is_empty(ds_record[0])
	else:
		das = 0
		null_das = 1
		def_das = 0

	#print ds_record
	print id, null_das, def_das

        sql_update = "UPDATE datasources SET dds_facet_2 = '%d', def_das = '%d' WHERE id = %d" % (dds, def_das, id)
        cursor_update.execute(sql_update)
#

