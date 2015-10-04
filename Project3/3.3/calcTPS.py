#!/usr/bin/python
import commands
import time

queries_1 = commands.getoutput("mysql -e 'show status like \"Queries\";' | tail -1 | awk '{print $2}'")
uptime_1 = commands.getoutput("mysql -e 'show status like \"Uptime\";' | tail -1 | awk '{print $2}'")

time.sleep(5)

queries_2 = commands.getoutput("mysql -e 'show status like \"Queries\";' | tail -1 | awk '{print $2}'")
uptime_2 = commands.getoutput("mysql -e 'show status like \"Uptime\";' | tail -1 | awk '{print $2}'")
queries = int(int(queries_2) - int(queries_1) - 6)
#print queries
uptime = int(int(uptime_2) - int(uptime_1))
#print uptime
QPS = float(queries/uptime)
TPS = float(QPS/16)
#print TPS
TPSUti = float((100*TPS)/(133.31))
#print TPSUti

ret,cmdout= commands.getstatusoutput("mon-put-data -m \"TPSUtilization\" --namespace DingService --dimensions \"instance=sysbench,servertype=MySQL\" --value " + str(TPSUti) + " -u Percent")

