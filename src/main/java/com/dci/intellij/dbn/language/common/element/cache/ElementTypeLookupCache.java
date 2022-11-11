package com.dci.intellij.dbn.language.common.element.cache;

import com.dci.intellij.dbn.common.index.IndexContainer;
import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.util.Compactables;
import com.dci.intellij.dbn.language.common.DBLanguage;
import com.dci.intellij.dbn.language.common.SharedTokenTypeBundle;
import com.dci.intellij.dbn.language.common.TokenType;
import com.dci.intellij.dbn.language.common.TokenTypeBundle;
import com.dci.intellij.dbn.language.common.element.ElementTypeBundle;
import com.dci.intellij.dbn.language.common.element.impl.ElementTypeBase;
import com.dci.intellij.dbn.language.common.element.impl.LeafElementType;
import com.dci.intellij.dbn.language.common.element.impl.WrappingDefinition;
import com.dci.intellij.dbn.language.common.element.util.NextTokenResolver;
import gnu.trove.THashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

public abstract class ElementTypeLookupCache<T extends ElementTypeBase>/* implements ElementTypeLookupCache<T>*/ {
    private final Latent<IndexContainer<TokenType>> nextPossibleTokens = Latent.basic(() -> computeNextPossibleTokens());
    protected final T elementType;

    ElementTypeLookupCache(T elementType) {
        this.elementType = elementType;
    }

    public void initialise() {
        IndexContainer<TokenType> tokenTypes = nextPossibleTokens.get();
        Compactables.compact(tokenTypes);
    }

    /**
     * This method returns all possible tokens (optional or not) which may follow current element.
     *
     * NOTE: to be used only for limited scope, since the tree walk-up
     * is done only until first named element is hit.
     * (named elements do not have parents)
     */
    public Set<TokenType> getNextPossibleTokens() {
        IndexContainer<TokenType> nextPossibleTokens = this.nextPossibleTokens.get();
        TokenTypeBundle tokenTypes = elementType.getLanguageDialect().getParserTokenTypes();
        return nextPossibleTokens == null ?
                Collections.emptySet() :
                nextPossibleTokens.elements(index -> tokenTypes.getTokenType(index));
    }

    public boolean isNextPossibleToken(TokenType tokenType) {
        IndexContainer<TokenType> nextPossibleTokens = this.nextPossibleTokens.get();
        return nextPossibleTokens != null && nextPossibleTokens.contains(tokenType);
    }

    @Nullable
    private IndexContainer<TokenType> computeNextPossibleTokens() {
        return NextTokenResolver.from(this.elementType).resolve();
    }

    protected DBLanguage getLanguage() {
        return elementType.getLanguage();
    }

    protected ElementTypeBundle getElementTypeBundle() {
        return elementType.getElementBundle();
    }

    protected SharedTokenTypeBundle getSharedTokenTypes() {
        return getLanguage().getSharedTokenTypes();
    }

    public void captureFirstPossibleTokens(Set<TokenType> bucket) {
        bucket.addAll(getFirstPossibleTokens());
    }

    public void captureFirstPossibleTokens(IndexContainer<TokenType> bucket) {
        bucket.addAll(getFirstPossibleTokens());
    }

    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context) {
        return captureFirstPossibleLeafs(context.reset(), null);
    }

    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context) {
        return captureFirstPossibleTokens(context.reset(), null);
    }

    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, @Nullable Set<LeafElementType> bucket) {
        WrappingDefinition wrapping = elementType.getWrapping();
        if (wrapping != null) {
            bucket = initBucket(bucket);
            bucket.add(wrapping.getBeginElementType());
        }
        return bucket;
    }

    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, @Nullable Set<TokenType> bucket) {
        WrappingDefinition wrapping = elementType.getWrapping();
        if (wrapping != null) {
            bucket = initBucket(bucket);
            bucket.add(wrapping.getBeginElementType().getTokenType());
        }
        return bucket;
    }

    public void registerLeaf(LeafElementType leaf, ElementTypeBase source) {
        ElementTypeBase parent = elementType.getParent();
        if (parent != null) {
            parent.getLookupCache().registerLeaf(leaf, elementType);
        }
    }

    <E> Set<E> initBucket(Set<E> bucket) {
        if (bucket == null) bucket = new THashSet<>();
        return bucket;
    }

    public abstract boolean containsToken(TokenType tokenType);

    public abstract boolean containsLeaf(LeafElementType elementType);

    public abstract Set<TokenType> getFirstPossibleTokens();

    public abstract Set<TokenType> getFirstRequiredTokens();

    public abstract boolean couldStartWithLeaf(LeafElementType elementType);

    public abstract boolean shouldStartWithLeaf(LeafElementType elementType);

    public abstract boolean couldStartWithToken(TokenType tokenType);

    public abstract Set<LeafElementType> getFirstPossibleLeafs();

    public abstract Set<LeafElementType> getFirstRequiredLeafs();

    public abstract boolean isFirstPossibleLeaf(LeafElementType elementType);

    public abstract boolean isFirstRequiredLeaf(LeafElementType elementType);

    public abstract boolean startsWithIdentifier();

    public abstract boolean isFirstPossibleToken(TokenType tokenType);

    public abstract boolean isFirstRequiredToken(TokenType tokenType);
}
