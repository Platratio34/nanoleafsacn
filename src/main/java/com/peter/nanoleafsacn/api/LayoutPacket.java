package com.peter.nanoleafsacn.api;

import java.util.HashMap;

import com.peter.webserver.data.JsonObj;
import com.peter.webserver.data.SerializableEnum;
import com.peter.webserver.data.SerializableObject;

@SerializableObject 
public class LayoutPacket {

    public int numPanels;
    public int sideLength;
    public PositionLayout[] positionData;

    public LayoutPacket() {
        
    }

    @SerializableObject 
    public static class PositionLayout {

        public PositionLayout() {
            
        }

        public int panelId;
        public int x;
        public int y;
        public int o;
        public ShapeType shapeType;
    }

    @SerializableEnum(serializer = "serialize", deserializer = "deserialize", byObject = true)
    public static enum ShapeType {
        SHAPES_HEXAGON(7),
        SHAPES_TRIANGLE(8),
        SHAPES_MINI_TRIANGLE(9),
        SHAPES_CONTROLLER(12),
                        
        UNKNOWN(-1);
        ;

        public final int id;
        private static HashMap<Integer, ShapeType> types;

        private ShapeType(int id) {
            this.id = id;
            setup();
        }

        private void setup() {
            if (types == null) {
                types = new HashMap<>();
            }
            types.put(id, this);
        }
        
        public JsonObj serialize() {
            return new JsonObj(id);
        }

        public static ShapeType deserialize(JsonObj json) {
            return types.getOrDefault(json.integer(), UNKNOWN);
        }
    }
}
