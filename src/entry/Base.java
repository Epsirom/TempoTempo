package entry;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Epsirom on 14/12/9.
 */
public class Base {
    public static Logger logger(Object obj){
        return LoggerFactory.getLogger(obj.getClass());
    }

    public static Logger logger(Class c) {
        return LoggerFactory.getLogger(c);
    }

    public static Logger logger(String s) {
        return LoggerFactory.getLogger(s);
    }
}
