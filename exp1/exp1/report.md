# <center>基于Joeq的数据流分析</center>
<center><font face="a" size=2> 2017011303 林俊峰</font></center>

###实验内容
#### 1.完成Mysolver
参考数据流框架伪代码实现，其中正向数据流分析和逆向数据流分析互为镜像对称的关系，只需完成一个之后对称地修改代码即可。需要注意的几个地方:
- 数据流分析的基本单位为`quad`，一个`entry`/`exit`对应多个`quad`(下面简称`entry quad`)，需要先通过判断`quad.predecessor1()` / `quad.successor1()` 是否包含`null`来找出所有这些`quad`，以完成初始化和最后对`entry`/`exit`的赋值
- 在作交汇运算时，需要有一个起始元来和其它元两两作交汇运算，一种方法是使用`newTempVar()`再调用`setToTop()`方法作为`1`元，还有就是根据当前`quad`是否为`entry quad`选择`IN`/`OUT`或者`preds[0].OUT`/`succs[0].IN`，注意遍历前要将集合中的`null`先删除

#### 2.完成ReachingDefs
参考`Liveness`, 只要做几个小地方的修改
- 将`String`类型的`变量`改为`Integer`类型的`定值点`(用于排序后有序输出)
- 修改数据流的方向为正向
- 根据`ReachingDefs`的传递函数修改`Transferfn`的`visitQuad`方法

#### 3.完成Faintness
参考`Liveness`
- 数据流方向不用改，逆向
- `Liveness`和`Faintness`大约是互补的关系，可知`IN(exit)`是全集，交汇运算应该是求交
- 在`Transferfn`中修改传递函数。根据`Faintness`的第一条定义不难得出传递函数为$f_{1q}(x)= def_q\cup(x-use_q)$。然而由于`Faitness`的第二条定义，在`visitMove`和`visitBinary`需要进行特判，若`dst`是`faint`的，则传递函数变为$f_{2q}(x)= def_q\cup x$。

#### 4.完成FaintnessTest
`README`中提供的测试代码
```
int foo()
{
    int x = 1;
    int y = x + 2;
    int z = x + y;
    return x;`
}
```
在程序实际运行中并不能得出`x`是`faint`变量的结论，因为最后一个`return x`在编译时被优化成了`return 1`，因此我作了如下修改:
```
int test2(int u) {
    int x = u;
    int y = x + 2;
    int z = x + y;
    return x;
}
```
用参数变量来初始化`x`，最后结果符合预期。

我的其它测例及注释包含在代码中
