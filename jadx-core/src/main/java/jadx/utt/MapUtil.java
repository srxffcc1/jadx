package jadx.utt;

import java.util.Map;

/**
 * Created by King6rf on 2017/6/29.
 */
public class MapUtil<T,D> {

    public   String map2String(Map<T,D> maps){
        String result="";
        for(Map.Entry<T,D> entry : maps.entrySet()){
            result=result+entry.getKey()+":"+entry.getKey();
        }
        System.out.println(result);
        return result;
    }
}
