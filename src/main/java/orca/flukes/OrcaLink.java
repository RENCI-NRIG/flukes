package orca.flukes;

import org.apache.commons.collections15.Factory;

public class OrcaLink {
    private double capacity;
    private double weight;
    private String name;

    public OrcaLink(String name) {
        this.name = name;
    }
    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }             
    
    public String toString() {
        return name;
    }
    
    public static class OrcaLinkFactory implements Factory<OrcaLink> {
        private static int linkCount = 0;
        private static double defaultWeight;
        private static double defaultCapacity;

        private static OrcaLinkFactory instance = new OrcaLinkFactory();
        
        private OrcaLinkFactory() {            
        }
        
        public static OrcaLinkFactory getInstance() {
            return instance;
        }
        
        public OrcaLink create() {
            String name = "Link" + linkCount++;
            OrcaLink link = new OrcaLink(name);
            link.setWeight(defaultWeight);
            link.setCapacity(defaultCapacity);
            return link;
        }    

        public static double getDefaultWeight() {
            return defaultWeight;
        }

        public static void setDefaultWeight(double aDefaultWeight) {
            defaultWeight = aDefaultWeight;
        }

        public static double getDefaultCapacity() {
            return defaultCapacity;
        }

        public static void setDefaultCapacity(double aDefaultCapacity) {
            defaultCapacity = aDefaultCapacity;
        }   
    }  
}
