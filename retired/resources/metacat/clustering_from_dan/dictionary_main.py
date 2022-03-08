#
#       Simple Python Main Application
#
import MySQLdb
#
from simple_clustering import number_of_path_facets
from simple_clustering import number_of_name_facets
from simple_clustering import facets
from simple_clustering import simple_form

db = MySQLdb.connect("localhost","root","","dapadl")
cursor = db.cursor()
cursor_update = db.cursor()

sql = """SELECT id,dds_facet_2 FROM datasources WHERE id <= 1408996"""

num_records = cursor.execute(sql)

name_cluster = {}

idx = 0

for i in range(num_records):
        record = cursor.fetchone()
        id = record[0]
        facet = record[1]

	if name_cluster.has_key(facet):
		x = name_cluster[facet]
	else:
		name_cluster[facet] = [ idx ]
		idx = idx + 1

num_records = cursor.execute(sql)

for i in range(num_records):
        record = cursor.fetchone()
        id = record[0]
        facet = record[1]

	if name_cluster.has_key(facet):
		facet_idx = name_cluster[facet]
		print facet, facet_idx[0]
        	sql_update = "UPDATE datasources SET dds2_idx = '%d' WHERE id = %d" % (facet_idx[0], id)
		print sql_update
        	cursor_update.execute(sql_update)
	else:
		print 'no record'




