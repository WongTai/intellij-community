package com.intellij.codeInsight.daemon.impl.quickfix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author cdr
 */
public class CreateCastExpressionFromInstanceofAction extends CreateLocalVarFromInstanceofAction {
  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    boolean available = super.isAvailable(project, editor, file);
    if (!available) return false;
    PsiInstanceOfExpression instanceOfExpression = getInstanceOfExpressionAtCaret(editor, file);
    if (instanceOfExpression == null) return false;
    PsiTypeElement checkType = instanceOfExpression.getCheckType();
    if (checkType == null) return false;
    PsiType type = checkType.getType();
    String castTo = type.getPresentableText();
    setText(CodeInsightBundle.message("cast.to.0", castTo));
    return true;
  }

  public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) {
    if (!CodeInsightUtilBase.prepareFileForWrite(file)) return;

    PsiInstanceOfExpression instanceOfExpression = getInstanceOfExpressionAtCaret(editor, file);
    assert instanceOfExpression.getContainingFile() == file : instanceOfExpression.getContainingFile() + "; file="+file;
    PsiElement decl = createAndInsertCast(instanceOfExpression);
    if (decl == null) return;
    decl = CodeStyleManager.getInstance(project).reformat(decl);
    CaretModel caretModel = editor.getCaretModel();
    caretModel.moveToOffset(decl.getTextRange().getEndOffset());
  }

  @Nullable
  private static PsiElement createAndInsertCast(final PsiInstanceOfExpression instanceOfExpression) throws IncorrectOperationException {
    PsiElementFactory factory = JavaPsiFacade.getInstance(instanceOfExpression.getProject()).getElementFactory();
    PsiExpressionStatement statement = (PsiExpressionStatement)factory.createStatementFromText("((a)b)", instanceOfExpression);

    PsiParenthesizedExpression paren = (PsiParenthesizedExpression)statement.getExpression();
    PsiTypeCastExpression cast = (PsiTypeCastExpression)paren.getExpression();
    PsiType castType = instanceOfExpression.getCheckType().getType();
    cast.getCastType().replace(factory.createTypeElement(castType));
    cast.getOperand().replace(instanceOfExpression.getOperand());

    PsiElement element = insertAtAnchor(instanceOfExpression, statement);
    return CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(element);
  }

  @NotNull
  public String getFamilyName() {
    return CodeInsightBundle.message("cast.expression");
  }
}