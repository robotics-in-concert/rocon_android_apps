
package com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list;

import java.util.Random;

import com.github.robotics_in_concert.rocon_android_apps.map_annotation.annotations_list.annotations.*;

public class MockDataProvider {
    // A utility method that generates random annotations
    public static Annotation getRandomAnnotation(String name) {
        Annotation annotation = null;
        Random random = new Random();
        int type = random.nextInt(5);
        switch (type) {
            case 0:
                annotation = new Column(name);
                break;
            case 1:
                annotation = new Wall(name);
                break;
            case 2:
                annotation = new Marker(name);
                break;
            case 3:
                annotation = new Table(name);
                break;
            case 4:
                annotation = new Location(name);
                break;
        }
        return annotation;
    }
}
