public class Song {
    private String titulo;
    private String genero;
    private String autor;
    private String idioma;
    private int anoLanzamiento;
    
    public Song() {}
    
    public Song(String titulo, String genero, String autor, String idioma, int anoLanzamiento) {
        this.titulo = titulo;
        this.genero = genero;
        this.autor = autor;
        this.idioma = idioma;
        this.anoLanzamiento = anoLanzamiento;
    }
    
    // Getters y Setters
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
    
    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }
    
    public int getAnoLanzamiento() { return anoLanzamiento; }
    public void setAnoLanzamiento(int anoLanzamiento) { this.anoLanzamiento = anoLanzamiento; }
    
    @Override
    public String toString() {
        return String.format("%s - %s (%d) [%s, %s]", 
            titulo, autor, anoLanzamiento, genero, idioma);
    }
}