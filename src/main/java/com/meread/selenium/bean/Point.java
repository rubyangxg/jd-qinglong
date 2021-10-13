package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

/**
 * Created by yangxg on 2021/10/13
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
public class Point {
    private int x;
    private int y;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
