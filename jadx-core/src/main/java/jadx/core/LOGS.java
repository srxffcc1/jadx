package jadx.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class LOGS {
    public static final boolean needshow=true;
    private static final Logger LOGSS = LoggerFactory.getLogger(LOGS.class);


    
    public static  String getName() {
        return null;
    }

    
    public static  boolean isTraceEnabled() {
        return false;
    }

    
    public static  void trace(String msg) {

    }

    
    public static  void trace(String format, Object arg) {

    }

    
    public static  void trace(String format, Object arg1, Object arg2) {

    }

    
    public static  void trace(String format, Object... arguments) {

    }

    
    public static  void trace(String msg, Throwable t) {

    }

    
    public static  boolean isTraceEnabled(Marker marker) {
        return false;
    }

    
    public static  void trace(Marker marker, String msg) {

    }

    
    public static  void trace(Marker marker, String format, Object arg) {

    }

    
    public static  void trace(Marker marker, String format, Object arg1, Object arg2) {

    }

    
    public static  void trace(Marker marker, String format, Object... argArray) {

    }

    
    public static  void trace(Marker marker, String msg, Throwable t) {

    }

    
    public static  boolean isDebugEnabled() {
        return false;
    }

    
    public static  void debug(String msg) {
        if(needshow){
            LOGSS.debug(msg);
        }
    }

    
    public static  void debug(String format, Object arg) {
        if(needshow){
            LOGSS.debug(format,arg);
        }
    }

    
    public static  void debug(String format, Object arg1, Object arg2) {
        if(needshow){
            LOGSS.debug(format,arg1,arg2);
        }
    }

    
    public static  void debug(String format, Object... arguments) {
        if(needshow){
            LOGSS.debug(format,arguments);
        }
    }

    
    public static  void debug(String msg, Throwable t) {
        if(needshow){
            LOGSS.debug(msg);
        }
    }

    
    public static  boolean isDebugEnabled(Marker marker) {
        return false;
    }

    
    public static  void debug(Marker marker, String msg) {
        if(needshow){
            LOGSS.debug(msg);
        }
    }

    
    public static  void debug(Marker marker, String format, Object arg) {
        if(needshow){
            LOGSS.debug(marker,format,arg);
        }
    }

    
    public static  void debug(Marker marker, String format, Object arg1, Object arg2) {
        if(needshow){
            LOGSS.debug(marker,format,arg1,arg2);
        }
    }

    
    public static  void debug(Marker marker, String format, Object... arguments) {
        if(needshow){
            LOGSS.debug(marker,format,arguments);
        }
    }

    
    public static  void debug(Marker marker, String msg, Throwable t) {
        if(needshow){
            LOGSS.debug(msg);
        }
    }

    
    public static  boolean isInfoEnabled() {
        return false;
    }

    
    public static  void info(String msg) {
        if(needshow){
            LOGSS.info(msg);
        }
    }

    
    public static  void info(String format, Object arg) {
        if(needshow){
            LOGSS.info(format,arg);
        }
    }

    
    public static  void info(String format, Object arg1, Object arg2) {
        if(needshow){
            LOGSS.info(format,arg1,arg2);
        }
    }

    
    public static  void info(String format, Object... arguments) {
        if(needshow){
            LOGSS.info(format,arguments);
        }
    }

    
    public static  void info(String msg, Throwable t) {
        if(needshow){
            LOGSS.info(msg);
        }
    }

    
    public static  boolean isInfoEnabled(Marker marker) {
        return false;
    }

    
    public static  void info(Marker marker, String msg) {
        if(needshow){
            LOGSS.info(msg);
        }
    }

    
    public static  void info(Marker marker, String format, Object arg) {
        if(needshow){
            LOGSS.info(marker,format,arg);
        }
    }

    
    public static  void info(Marker marker, String format, Object arg1, Object arg2) {
        if(needshow){
            LOGSS.info(marker,format,arg1,arg2);
        }
    }

    
    public static  void info(Marker marker, String format, Object... arguments) {
        if(needshow){
            LOGSS.info(marker,format,arguments);
        }
    }

    
    public static  void info(Marker marker, String msg, Throwable t) {
        if(needshow){
            LOGSS.info(msg);
        }
    }

    
    public static  boolean isWarnEnabled() {
        return false;
    }

    
    public static  void warn(String msg) {
        if(needshow){
            LOGSS.warn(msg);
        }
    }

    
    public static  void warn(String format, Object arg) {
        if(needshow){
            LOGSS.warn(format,arg);
        }
    }

    
    public static  void warn(String format, Object... arguments) {
        if(needshow){
            LOGSS.warn(format,arguments);
        }
    }

    
    public static  void warn(String format, Object arg1, Object arg2) {
        if(needshow){
            LOGSS.warn(format,arg1,arg2);
        }
    }

    
    public static  void warn(String msg, Throwable t) {
        if(needshow){
            LOGSS.warn(msg);
        }
    }

    
    public static  boolean isWarnEnabled(Marker marker) {
        return false;
    }

    
    public static  void warn(Marker marker, String msg) {
        if(needshow){
            LOGSS.warn(msg);
        }
    }

    
    public static  void warn(Marker marker, String format, Object arg) {
        if(needshow){
            LOGSS.warn(marker,format,arg);
        }
    }

    
    public static  void warn(Marker marker, String format, Object arg1, Object arg2) {
        if(needshow){
            LOGSS.warn(marker,format,arg1,arg2);
        }
    }

    
    public static  void warn(Marker marker, String format, Object... arguments) {
        if(needshow){
            LOGSS.warn(marker,format,arguments);
        }
    }

    
    public static  void warn(Marker marker, String msg, Throwable t) {
        if(needshow){
            LOGSS.warn(msg);
        }
    }

    
    public static  boolean isErrorEnabled() {
        return false;
    }

    
    public static  void error(String msg) {
        if(needshow){
            LOGSS.error(msg);
        }
    }

    
    public static  void error(String format, Object arg) {
        if(needshow){
            LOGSS.error(format,arg);
        }
    }

    
    public static  void error(String format, Object arg1, Object arg2) {
        if(needshow){
            LOGSS.error(format,arg1,arg2);
        }
    }

    
    public static  void error(String format, Object... arguments) {
        if(needshow){
            LOGSS.error(format,arguments);
        }
    }

    
    public static  void error(String msg, Throwable t) {
        if(needshow){
            LOGSS.error(msg);
        }
    }

    
    public static  boolean isErrorEnabled(Marker marker) {
        return false;
    }

    
    public static  void error(Marker marker, String msg) {
        if(needshow){
            LOGSS.error(msg);
        }
    }

    
    public static  void error(Marker marker, String format, Object arg) {
        if(needshow){
            LOGSS.error(marker,format,arg);
        }
    }

    
    public static  void error(Marker marker, String format, Object arg1, Object arg2) {
        if(needshow){
            LOGSS.error(marker,format,arg1,arg2);
        }
    }

    
    public static  void error(Marker marker, String format, Object... arguments) {
        if(needshow){
            LOGSS.error(marker,format,arguments);
        }
    }

    
    public static  void error(Marker marker, String msg, Throwable t) {
        if(needshow){
            LOGSS.error(msg);
        }
    }
}
