#
#       Simple Python Main Application
#
import MySQLdb
#
from simple_clustering import number_of_path_facets
from simple_clustering import number_of_name_facets
from simple_clustering import facets
from simple_clustering import simple_form
from simple_clustering import special_char_form
from simple_clustering import alpha_char_form

db = MySQLdb.connect("localhost","root","","dapadl")
cursor = db.cursor()
cursor_update = db.cursor()

sql = """SELECT name FROM datasources WHERE id <= 1408996"""

cursor.execute(sql)

for i in range(1408996):
        record = cursor.fetchone()
        path = record[0]

        path_parts = path.split('/')
        #n_facet = lexform(path_parts[-1])
        p_facet = number_of_path_facets(path) - 2
        #s_facet = special_char_form(path_parts[-1])
        #a_facet = alpha_char_form(path_parts[-1])

        sql_update = "UPDATE datasources SET path_facets = '%d' WHERE id = %d" % (p_facet, i+1)
        cursor_update.execute(sql_update)
