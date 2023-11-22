
slots = 8;

difference() {
    union() {
        // Grundplatte
        cube( [(slots + 1) * 12.7, 73, 3.5] );
        
        // vordere Halterung
        translate([0, -5, 2]) {
            cube( [(slots + 1) * 12.7, 8, 1] );
        }
    }
    
    // Platinenführungen
    for (i=[1:slots]) {
        translate([i * 12.7 - 1, -6, 2]) {
            cube( [2, 78.6, 5] );
        }
    }
    
    // Aussparungen
    for (i=[0:slots]) {
        translate([i * 12.7 + 2, 5, -0.1]) {
            cube( [8.7, 62, 5] );
        }
    }
    
}