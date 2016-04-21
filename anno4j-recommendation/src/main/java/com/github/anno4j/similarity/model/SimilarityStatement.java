package com.github.anno4j.similarity.model;

import com.github.anno4j.similarity.ontologies.ANNO4JREC;
import org.openrdf.annotations.Iri;

/**
 * Extends a statement, automatically setting the predicate.
 */
@Iri(ANNO4JREC.SIMILARITY_STATEMENT)
public interface SimilarityStatement extends Statement {

    @Iri(ANNO4JREC.BASED_ON)
    Similarity getSimilarity();

    @Iri(ANNO4JREC.BASED_ON)
    void setSimilarity(Similarity similarity);

    @Iri(ANNO4JREC.HAS_SIMILARITY_VALUE)
    double getSimilarityValue();

    @Iri(ANNO4JREC.HAS_SIMILARITY_VALUE)
    void setSimilarityValue(double value);
}