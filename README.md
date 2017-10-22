## 简介

这是放即刻app点赞效果的Demo，效果如下：

![效果图](http://upload-images.jianshu.io/upload_images/3157525-01193b0e7379013d.gif?imageMogr2/auto-orient/strip)

[例子.apk](https://github.com/arvinljw/ThumbUpSample/tree/master/app/app-release.apk)

## 原理

**简单介绍一下：**

这个点赞效果，经过我反复观察之后分析如下：

### 1、结构

**左边：** 由三个图片组成，分为未点赞和已点赞两种状态，未点赞时只有一个灰色的拇指，点赞后是彩色拇指和一个散开的点，变化过程是一系列动画（稍后细说）；

**右边：** 是点赞的数量，由于有变化的动画，我分成了三部分，不变的部分，变化前的部分以及变化后的部分，这里就涉及到一个简单的算法（稍后细说）去得出这些部分的文本，方便绘制。


### 2、动画

组成部分介绍完了，接下来再来看看，动画的细分：

**左边：** 从未点赞到点赞变化是：a.灰色拇指变小，b.再变成彩色拇指，从刚才的大小变到原始大小，同时c.彩色圆圈从中间扩散开，扩散的时候散开的点也逐渐显示出来（这一点使用clipPath就能实现）；动画执行顺序是先执行a再b,c一起执行；本例子使用的是自定义属性动画实现，具体代码就不解释了都是HenCoder中介绍过的知识。

**右边：** 先计算出不变的部分，变化前的部分以及变化后的部分，如果是变大，变化前的部分就从基准位置向上移动，变化后的部分就从下方往上移动到基准位置；如果是变小，变化前的部分就从基准位置向下移动，变化后的部分就从上方往下移动刀基准位置；这一部分的动画也是使用属性动画完成的，相当于只需要控制一个偏移量即可，具体的变化如下：

```
public void setTextOffsetY(float offsetY) {
    this.mOldOffsetY = offsetY;//变大是从[0,1]，变小是[0,-1]
    if (toBigger) {//从下到上[-1,0]
        this.mNewOffsetY = OFFSET_MAX + offsetY;
    } else {//从上到下[1,0]
        this.mNewOffsetY = offsetY - OFFSET_MAX;
    }
    postInvalidate();
}
```

### 算法

说的这么高大上，其实也不难，就是拿到变化前的数量的变化后的数量，转化成字符串，如果这两个数量的长度不一样，则表示全部都会变化，所以不变的部分就是""，变化前的部分就是变化前的数量，变化后的部分就是变化后的数量；如果这两个数量的长度一致，则从字符串的第一位开始往下比较，直到不一样的时候，就表示从这一位数到最后一位数都表示需要变化，然后使用字符串的截取即可获得，不变的部分就是从第一位到这一位，变化前的就是用变化前的字符串从这一位开始截取到最后一位，变化后的也是同理。

文字看着挺多，相信肯定还是直接看代码来得直接：

```
/**
 * 计算不变，原来，和改变后各部分的数字
 * 这里是只针对加一和减一去计算的算法，因为直接设置的时候没有动画
 */
private void calculateChangeNum(int change) {
    if (change == 0) {
        nums[0] = String.valueOf(count);
        nums[1] = "";
        nums[2] = "";
        return;
    }
    toBigger = change > 0;
    String oldNum = String.valueOf(count);
    String newNum = String.valueOf(count + change);
    int oldNumLen = oldNum.length();
    int newNumLen = newNum.length();
    if (oldNumLen != newNumLen) {
        nums[0] = "";
        nums[1] = oldNum;
        nums[2] = newNum;
    } else {
        for (int i = 0; i < oldNumLen; i++) {
            char oldC1 = oldNum.charAt(i);
            char newC1 = newNum.charAt(i);
            if (oldC1 != newC1) {
                if (i == 0) {
                    nums[0] = "";
                } else {
                    nums[0] = newNum.substring(0, i);
                }
                nums[1] = oldNum.substring(i);
                nums[2] = newNum.substring(i);
                break;
            }
        }
    }
}
```

## 其他

这次的模仿也不尽完善，比如快速点击点赞和取消点赞时，动画效果不一致；点赞时其实散开的点有点放大的效果，也没有去处理。若是有好的建议和想法，能够指教一二，就不胜感激了。

最后感谢HenCoder，确实对我在自定义View的细节上有不少帮助。

