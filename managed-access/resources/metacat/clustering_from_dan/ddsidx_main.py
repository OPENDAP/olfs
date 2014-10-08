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

sql = """SELECT dds_facet FROM datasources WHERE id <= 10000"""

num_records = cursor.execute(sql)

name_cluster = {}

for i in range(num_records):
        record = cursor.fetchone()
        facet = record[0]

	if name_cluster.has_key(facet):
		name_cluster[facet] = name_cluster[facet] + 1
	else:
		name_cluster[facet] =  1

dds = name_cluster.keys()
ddssz = name_cluster.values()

for i in range(len(name_cluster)):
	print i, dds[i], ddssz[i]


