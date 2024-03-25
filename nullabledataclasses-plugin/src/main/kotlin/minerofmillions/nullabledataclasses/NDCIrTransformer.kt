package minerofmillions.nullabledataclasses

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.expressions.putArgument
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private const val DEBUG = false

class NDCIrTransformer(
  private val pluginContext: IrPluginContext,
  private val annotationClass: IrClassSymbol,
) : IrElementTransformerVoidWithContext() {
  override fun visitClassNew(declaration: IrClass): IrStatement {
    if (declaration.hasAnnotation(annotationClass)) {
      val currentToString = declaration.functions.find { it.name.asString() == "toString" }
      if (currentToString == null) {
        declaration.addFunction {
          name = Name.identifier("toString")
          returnType = pluginContext.irBuiltIns.stringType
        }.also {
          it.body = irToString(it.symbol, declaration, null)
        }
      } else {
        if (DEBUG) currentToString.transform(TempTransformer(pluginContext), currentToString.dump())
        else currentToString.transform(this, null)
      }
    }
    return declaration
  }

  override fun visitFunctionNew(declaration: IrFunction): IrStatement {
    assert(declaration.name == Name.identifier("toString"))
    declaration.body = irToString(declaration.symbol, declaration.parentAsClass, declaration.dispatchReceiverParameter)
    return declaration
  }

  private fun irToString(symbol: IrSymbol, klass: IrClass, thisParameter: IrValueParameter?) =
    DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
      val irThis = thisParameter?.let(::irGet)
      val irSeparator = irString(", ")

      val funStringPlus =
        pluginContext.referenceFunctions(CallableId(FqName("kotlin"), FqName("String"), Name.identifier("plus")))
          .single()

      val irHadParameter = irTemporary(irFalse(), isMutable = true)
      val irGetHadParameter = irGet(irHadParameter)

      val concat = irConcat()
      concat.addArgument(irString("${klass.name.asString()}("))

      klass.properties.filter { it.visibility == DescriptorVisibilities.DEFAULT_VISIBILITY }.toList()
        .mapNotNull { property ->
          property.getter?.let {
            val value = irCall(it).apply {
              dispatchReceiver = irThis
            }
            val localConcat = irConcat()
            localConcat.addArgument(irString("${property.name.asString()}="))
            localConcat.addArgument(value)
            irIfThenElse(context.irBuiltIns.stringType, irEqualsNull(value), irString(""), irIfThenElse(context.irBuiltIns.stringType, irGetHadParameter, irCall(funStringPlus).apply {
              dispatchReceiver = irSeparator
              putArgument(this.symbol.owner.valueParameters[0], localConcat)
            }, irBlock(resultType = context.irBuiltIns.stringType) {
              +irSet(irHadParameter, irTrue())
              +localConcat
            }))
          }
        }.forEach(concat::addArgument)

      concat.addArgument(irString(")"))
      +irReturn(concat)
    }
}

private class TempTransformer(private val context: IrPluginContext) : IrElementTransformer<String> {
  override fun visitFunction(declaration: IrFunction, data: String): IrStatement {
    declaration.body = DeclarationIrBuilder(context, declaration.symbol).let {
      it.irExprBody(it.irString(data))
    }
    return declaration
  }
}
