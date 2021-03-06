// This is a generated file. Not intended for manual editing.
package com.jetbrains.ther.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.jetbrains.ther.parsing.TheRElementTypes.*;
import com.jetbrains.ther.psi.api.*;

public class TheRMemberExpressionImpl extends TheRExpressionImpl implements TheRMemberExpression {

  public TheRMemberExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof TheRVisitor) ((TheRVisitor)visitor).visitMemberExpression(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public TheRExpression getExpression() {
    return findNotNullChildByClass(TheRExpression.class);
  }

  @Override
  @NotNull
  public PsiElement getListSubset() {
    return findNotNullChildByType(THE_R_LIST_SUBSET);
  }

  @Override
  @Nullable
  public PsiElement getIdentifier() {
    return findChildByType(THE_R_IDENTIFIER);
  }

  @Override
  @Nullable
  public PsiElement getString() {
    return findChildByType(THE_R_STRING);
  }

  public String getTag() {
    return TheRPsiImplUtil.getTag(this);
  }

}
