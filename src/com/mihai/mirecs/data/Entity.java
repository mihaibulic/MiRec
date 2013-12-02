package com.mihai.mirecs.data;

import java.util.Comparator;

public class Entity {
    public long mId;
    public String mName;
    public String mPicture;
    public int mScore;

    public static class EntityComparator implements Comparator<Entity> {
        @Override
        public int compare(Entity lhs, Entity rhs) {
            return rhs.mScore - lhs.mScore;
        }
    }

    public Entity(long id) {
        this.mId = id;
        this.mScore = 0;
    }

    public Entity(long id, String name, String picture, int score) {
        this.mId = id;
        this.mName = name;
        this.mPicture = picture;
        this.mScore = score;
    }

    @Override
    public String toString() {
        return mName;
    }
}
