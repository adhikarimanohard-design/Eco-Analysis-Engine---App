import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;

public class Database {
    private static final String DB_URL = "jdbc:h2:./binovatorDB";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";
    private static Database instance = null;

    private Database() {
        createDatabase();
    }

    public static synchronized Database getInstance() {
        if (instance == null) instance = new Database();
        return instance;
    }

    private void createDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS products(" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "material DOUBLE, energy DOUBLE, waste DOUBLE, " +
                    "longevity DOUBLE, social DOUBLE, si DOUBLE, grade VARCHAR(5))");
        } catch (SQLException e) {
            System.err.println("DB setup error: " + e.getMessage());
        }
    }

    public int insertProduct(product p) throws SQLException {
        String SQL = "INSERT INTO products(name, material, energy, waste, longevity, social, si, grade) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setDouble(2, p.getMaterial());
            ps.setDouble(3, p.getEnergy());
            ps.setDouble(4, p.getWaste());
            ps.setDouble(5, p.getLongevity());
            ps.setDouble(6, p.getSocial());
            ps.setDouble(7, p.getSi());
            ps.setString(8, p.getGrade());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public void deleteProduct(product p) throws SQLException {
        String SQL = "DELETE FROM products WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setInt(1, p.getId());
            ps.executeUpdate();
        }
    }

    public ObservableList<product> loadProducts() throws SQLException {
        ObservableList<product> productList = FXCollections.observableArrayList();
        String SQL = "SELECT * FROM products";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL)) {
            while (rs.next()) {
                product p = new product(
                        rs.getInt("id"), rs.getString("name"),
                        rs.getDouble("material"), rs.getDouble("energy"),
                        rs.getDouble("waste"), rs.getDouble("longevity"),
                        rs.getDouble("social"), rs.getDouble("si"),
                        rs.getString("grade")
                );
                productList.add(p);
            }
        }
        return productList;
    }
}