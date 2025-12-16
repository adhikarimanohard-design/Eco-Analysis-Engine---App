public class product {
    private int id;
    private String name;
    private double material, energy, waste, longevity, social, si;
    private String grade;

    public product(int id, String name, double material, double energy, double waste,
                   double longevity, double social, double si, String grade) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.energy = energy;
        this.waste = waste;
        this.longevity = longevity;
        this.social = social;
        this.si = si;
        this.grade = grade;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public double getMaterial() { return material; }
    public double getEnergy() { return energy; }
    public double getWaste() { return waste; }
    public double getLongevity() { return longevity; }
    public double getSocial() { return social; }
    public double getSi() { return si; }
    public String getGrade() { return grade; }
}