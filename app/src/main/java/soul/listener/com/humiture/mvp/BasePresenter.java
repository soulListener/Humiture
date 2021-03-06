package soul.listener.com.humiture.mvp;
/**
 * @description Presenter基类
 */

public class BasePresenter<V> {
    public V mView;

    public BasePresenter() {

    }

    public BasePresenter(V v) {
        attachView(v);
    }

    public void attachView(V view) {
        this.mView = view;
    }

    public void detachView() {
        this.mView = null;
    }

}
