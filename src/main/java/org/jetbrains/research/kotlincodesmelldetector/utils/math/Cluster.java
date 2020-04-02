package org.jetbrains.research.kotlincodesmelldetector.utils.math;

import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.kotlin.psi.KtElement;

import java.util.ArrayList;

public class Cluster {

    private final ArrayList<SmartPsiElementPointer<? extends KtElement>> entities;
    private int hashCode;

    public Cluster() {
        entities = new ArrayList<>();
    }

    public Cluster(ArrayList<SmartPsiElementPointer<? extends KtElement>> entities) {
        this.entities = new ArrayList<>(entities);
    }

    public void addEntity(SmartPsiElementPointer<? extends KtElement> entity) {
        if (!entities.contains(entity)) {
            entities.add(entity);
        }
    }

    public ArrayList<SmartPsiElementPointer<? extends KtElement>> getEntities() {
        return entities;
    }

    public void addEntities(ArrayList<SmartPsiElementPointer<? extends KtElement>> entities) {
        if (!this.entities.containsAll(entities)) {
            this.entities.addAll(entities);
        }
    }

    /*
    //TODO remove if there is no usage

    public boolean equals(Object o) {
        Cluster c = (Cluster) o;
        return this.entities.equals(c.entities);
    }

     */

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            for (SmartPsiElementPointer<? extends KtElement> entity : entities) {
                KtElement element = entity.getElement();
                if (element != null) {
                    result = 37 * result + entity.getElement().hashCode();
                }
            }

            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder s = new StringBuilder("{");

        for (SmartPsiElementPointer<? extends KtElement> entity : entities) {
            s.append(entity).append(", ");
        }
        s.append("}");
        return s.toString();
    }
}