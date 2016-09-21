/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * add by machao
 */
public class IndexUtils {
    
    private static String regexDtYMDH = ".*?(20\\d{2})[-|_]*(\\d{2})[-|_]*(\\d{2})[-|_]*(\\d{2})";
    private static String regexDtYMD = ".*?(20\\d{2})[-|_]*(\\d{2})[-|_]*(\\d{2})";
    private static String regexDtYM = ".*?(20\\d{2})[-|_]*(\\d{2})";
    private static String regexDtY = ".*?(20\\d{2})";
    private static Pattern patternDTYMDH = Pattern.compile(regexDtYMDH, Pattern.DOTALL);
    private static Pattern patternDTYMD = Pattern.compile(regexDtYMD, Pattern.DOTALL);
    private static Pattern patternDTYM = Pattern.compile(regexDtYM, Pattern.DOTALL);
    private static Pattern patternDTY = Pattern.compile(regexDtY, Pattern.DOTALL);
    
    private static Pattern patternNow = Pattern.compile("now-(.*)(.)");
    
    /**
     * @param indexName
     * @return
     * @throws ParseException 
     */
    private static String[] parseDateByIndex(String indexName) throws ParseException {
        String[] result = new String[2];
        //按照小时匹配
        Matcher matcher = patternDTYMDH.matcher(indexName);
        if(matcher.find()) {
            int groupCount = matcher.groupCount();
            String dateString = null;
            for(int i = 1; i <= groupCount; i++) {
                if(i == 1) {
                    dateString = matcher.group(i);
                } else {
                    dateString = dateString + matcher.group(i);
                }
            }
            result[0] = dateString;
            result[1] = "yyyyMMddHH";
            return result;
        }
        //按照天匹配
        matcher = patternDTYMD.matcher(indexName);
        if(matcher.find()) {
            int groupCount = matcher.groupCount();
            String dateString = null;
            for(int i = 1; i <= groupCount; i++) {
                if(i == 1) {
                    dateString = matcher.group(i);
                } else {
                    dateString = dateString + matcher.group(i);
                }
            }
            result[0] = dateString;
            result[1] = "yyyyMMdd";
            return result;
        }
        //按照月匹配
        matcher = patternDTYM.matcher(indexName);
        if(matcher.find()) {
            int groupCount = matcher.groupCount();
            String dateString = null;
            for(int i = 1; i <= groupCount; i++) {
                if(i == 1) {
                    dateString = matcher.group(i);
                } else {
                    dateString = dateString + matcher.group(i);
                }
            }
            result[0] = dateString;
            result[1] = "yyyyMM";
            return result;
        }
        //按照年匹配
        matcher = patternDTY.matcher(indexName);
        if(matcher.find()) {
            int groupCount = matcher.groupCount();
            String dateString = null;
            for(int i = 1; i <= groupCount; i++) {
                if(i == 1) {
                    dateString = matcher.group(i);
                } else {
                    dateString = dateString + matcher.group(i);
                }
            }
            result[0] = dateString;
            result[1] = "yyyy";
           return result;
        }
        return result;
    }
    
    /**
     * 按照索引名字和from to 修改时间范围
     * @param from
     * @param to
     * @param indexName
     * @return
     */
    public static Object[] parseTimeRangeByIndex(String from, String to, String indexName) {
        Object[] from_to = new Object[]{from, to};
        long currentMills = System.currentTimeMillis();
        try {
            String[] formatArr = parseDateByIndex(indexName);
            if(formatArr == null) {
                return from_to;
            }
            
            String dateString = formatArr[0];
            String formatString = formatArr[1];
            
            Date index_date = new SimpleDateFormat(formatString, Locale.ENGLISH).parse(dateString);
            
            //开始毫秒
            long index_date_start_mills = index_date.getTime();
            //结束毫秒
            long index_date_end_mills = 0;
            
            if(formatString.endsWith("H")) {
                index_date_end_mills = index_date_start_mills + 1 * 60 * 60 * 1000 - 1;
            } else if(formatString.endsWith("d")) {
                index_date_end_mills = index_date_start_mills + 24 * 60 * 60 * 1000 - 1;
            } else if(formatString.endsWith("M")) {
                index_date_end_mills = index_date_start_mills + 30 * 24 * 60 * 60 * 1000 - 1;
            } else if(formatString.endsWith("y")) {
                index_date_end_mills = index_date_start_mills + 365 * 30 * 24 * 60 * 60 * 1000 - 1;
            }
            
            
            //from和to中包含now的类型
            if(to != null && to.equals("now")) {
                Matcher matcher = patternNow.matcher(from);
                if(matcher.find() && matcher.groupCount() == 2) {
                    String numString = matcher.group(1);
                    int num = Integer.parseInt(numString);
                    String unit = matcher.group(2);
                    if(unit.equalsIgnoreCase("d")) {
                        long startMills = currentMills - num * 24 * 60 * 60 * 1000;
                        if(startMills < index_date_start_mills) {
                            from_to[0] = index_date_start_mills;
                            from_to[1] = index_date_end_mills;
                            return from_to;
                        } else {
                            return from_to;
                        }
                    } else if(unit.equalsIgnoreCase("H")) {
                        long startMills = currentMills - num * 60 * 60 * 1000;
                        if(startMills < index_date_start_mills) {
                            from_to[0] = index_date_start_mills;
                            from_to[1] = index_date_end_mills;
                            return from_to;
                        } else {
                            return from_to;
                        }
                    } else if(unit.equals("m")) {
                        long startMills = currentMills - num * 60 * 1000;
                        if(startMills < index_date_start_mills) {
                            from_to[0] = index_date_start_mills;
                            from_to[1] = index_date_end_mills;
                            return from_to;
                        } else {
                            return from_to;
                        }
                    }
                }
            }
            
            //from和to都是时间戳类型
            if(from != null && from.contains("T") && to != null && to.contains("T")) {
                long startMills = parseCreateTime(from);
                long endMills = parseCreateTime(to);
                if(startMills < index_date_start_mills && endMills > index_date_end_mills) {
                    from_to[0] = index_date_start_mills;
                    from_to[1] = index_date_end_mills;
                }
                return from_to;
            }
            
            //from是时间类型，to是now
            if(from != null && from.contains("T") && to != null && to.equalsIgnoreCase("now")) {
                long startMills = parseCreateTime(from);
                long endMills = currentMills;
                if(startMills < index_date_start_mills && endMills > index_date_end_mills) {
                    from_to[0] = index_date_start_mills;
                    from_to[1] = index_date_end_mills;
                } else if(startMills > index_date_start_mills && endMills > index_date_end_mills) {
                    from_to[0] = startMills;
                    from_to[1] = index_date_end_mills;
                }
                return from_to;
            }
            
            //from和to都是毫秒值类型
            if(Long.parseLong(from) < index_date_start_mills && Long.parseLong(to) > index_date_end_mills) {
                from_to[0] = index_date_start_mills;
                from_to[1] = index_date_end_mills;
                return from_to;
            }
            
        } catch (Exception e) {
           // e.printStackTrace();
            return from_to;
        }
        return from_to;
    }
    
    /**
     * 解析时间戳
     * @param timeStr
     * @return
     */
    private static long parseCreateTime(String timeStr) {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        DateTime dateTime2 = DateTime.parse(timeStr, formatter);
        return dateTime2.getMillis();
    }
    
    public static void main(String[] args) throws ParseException {
       // String from = "2016-07-02T10:37:51.731Z";
        //String to = "2016-07-02T10:37:51.731Z";
        
        // BytesRef br = BytesRefs.toBytesRef("2016-07-02T10:37:51.731Z");
         //System.out.println(BytesRefs.toString(br));
         
         //System.exit(0);
        
        //Object[] objArr = parseTimeRangeByIndex("now-15d", "now", "es_index_acp20160704");
        //Object[] objArr = parseTimeRangeByIndex("2016-07-02T10:37:51.731Z", "2016-07-04T10:37:51.731Z", "es_index_acp20160702");
        //Object[] objArr = parseTimeRangeByIndex("2016-07-02T10:37:51.731Z", "now", "es_index_acp20160702");
        //Object[] objArr = parseTimeRangeByIndex("-2790000000", "148025808000000", "es_index_ceshi2");
        //System.out.println(objArr[0].toString());
        //System.out.println(objArr[1].toString());
        //System.out.println(new Date(Long.parseLong(objArr[0].toString())));
        //System.out.println(new Date(Long.parseLong(objArr[1].toString())));
    }
}
