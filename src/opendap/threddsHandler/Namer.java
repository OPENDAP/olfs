package opendap.threddsHandler;

import opendap.namespaces.THREDDS;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ndp on 4/18/15.
 */
public class Namer {
    Vector<NamerWorker> _workers;


    public Namer(Element namer) {
        _workers = new Vector<>();
        if (namer != null) {
            for (Object o : namer.getChildren()) {

                Element workerElement = (Element) o;

                NamerWorker clude = new NamerWorker(workerElement);

                _workers.add(clude);

            }
        }

    }


    public class NamerWorker {

        private Logger _log;
        String regExp;
        String replaceString;

        boolean nameFromPathMatch;

        Pattern regExpPattern;

        public NamerWorker(Element worker) {

            _log = LoggerFactory.getLogger(this.getClass());

            if (worker == null ||
                    (!worker.getName().equals(THREDDS.REG_EXP_ON_NAME) && !worker.getName().equals(THREDDS.REG_EXP_ON_PATH)))
                return;

            nameFromPathMatch = worker.getName().equals(THREDDS.REG_EXP_ON_PATH);


            regExp = worker.getAttributeValue(THREDDS.REGEXP);
            if (regExp != null) {
                regExpPattern = Pattern.compile(regExp);
            } else {
                regExpPattern = Pattern.compile(".*$");

            }


            replaceString = worker.getAttributeValue(THREDDS.REPLACE_STRING);


        }


        String getName(String name) {

            if (name == null)
                return null;

            Matcher matcher;
            if (nameFromPathMatch) {
                _log.error("getName() - {} not supported.", THREDDS.REG_EXP_ON_PATH);
                return null;
            } else {
                matcher = this.regExpPattern.matcher(name);
            }

            if (!matcher.find())
                return null;

            StringBuffer newName = new StringBuffer();
            matcher.appendReplacement(newName, this.replaceString);
            newName.delete(0, matcher.start());

            if (newName.length() == 0) return null;

            return newName.toString();

        }

    }

    public String getName(String name) {


        String newName = null;
        NamerWorker worker;
        Iterator<NamerWorker> i = _workers.iterator();

        while (newName == null && i.hasNext()) {
            worker = i.next();
            newName = worker.getName(name);
        }


        return newName;

    }

}
