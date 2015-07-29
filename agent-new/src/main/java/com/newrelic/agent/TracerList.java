package com.newrelic.agent;

import com.newrelic.agent.tracers.Tracer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class TracerList
  implements List<Tracer>
{
  private final Set<TransactionActivity> activities;
  private List<Tracer> tracers;
  private final Tracer txRootTracer;

  public TracerList(Tracer txRootTracer, Set<TransactionActivity> activities)
  {
    if (activities == null) {
      throw new IllegalArgumentException();
    }
    this.activities = activities;
    this.txRootTracer = txRootTracer;
  }

  private List<Tracer> getTracers()
  {
    if (this.tracers == null) {
      int n = 0;
      for (TransactionActivity txa : this.activities) {
        n += txa.getTracers().size();
      }
      n++;
      this.tracers = new ArrayList(n);
      for (TransactionActivity txa : this.activities) {
        if (txa.getRootTracer() != this.txRootTracer) {
          this.tracers.add(txa.getRootTracer());
        }
        this.tracers.addAll(txa.getTracers());
      }
    }
    return this.tracers;
  }

  public int size()
  {
    return getTracers().size();
  }

  public boolean isEmpty()
  {
    return getTracers().isEmpty();
  }

  public boolean contains(Object o)
  {
    return getTracers().contains(o);
  }

  public Iterator<Tracer> iterator()
  {
    return getTracers().iterator();
  }

  public Object[] toArray()
  {
    return getTracers().toArray();
  }

  public <T> T[] toArray(T[] a)
  {
    return getTracers().toArray(a);
  }

  public boolean add(Tracer e)
  {
    throw new UnsupportedOperationException();
  }

  public boolean remove(Object o)
  {
    return getTracers().remove(o);
  }

  public boolean containsAll(Collection<?> c)
  {
    return getTracers().containsAll(c);
  }

  public boolean addAll(Collection<? extends Tracer> c)
  {
    return getTracers().addAll(c);
  }

  public boolean addAll(int index, Collection<? extends Tracer> c)
  {
    return getTracers().addAll(index, c);
  }

  public boolean removeAll(Collection<?> c)
  {
    return getTracers().removeAll(c);
  }

  public boolean retainAll(Collection<?> c)
  {
    return getTracers().retainAll(c);
  }

  public void clear()
  {
    getTracers().clear();
  }

  public Tracer get(int index)
  {
    return (Tracer)getTracers().get(index);
  }

  public Tracer set(int index, Tracer element)
  {
    return (Tracer)getTracers().set(index, element);
  }

  public void add(int index, Tracer element)
  {
    getTracers().add(index, element);
  }

  public Tracer remove(int index)
  {
    return (Tracer)getTracers().remove(index);
  }

  public int indexOf(Object o)
  {
    return getTracers().indexOf(o);
  }

  public int lastIndexOf(Object o)
  {
    return getTracers().lastIndexOf(o);
  }

  public ListIterator<Tracer> listIterator()
  {
    return getTracers().listIterator();
  }

  public ListIterator<Tracer> listIterator(int index)
  {
    return getTracers().listIterator(index);
  }

  public List<Tracer> subList(int fromIndex, int toIndex)
  {
    return getTracers().subList(fromIndex, toIndex);
  }
}