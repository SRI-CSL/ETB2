package etb.etbDL.engine;

//interface for an object that can be indexed for use with {IndexedSet}, where <T> is the index type
public interface Indexable<T> {
    //returns the index of this instance object
    T index();
}
