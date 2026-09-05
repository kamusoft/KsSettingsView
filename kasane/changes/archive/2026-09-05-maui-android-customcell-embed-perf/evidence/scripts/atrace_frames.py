import sys,zlib,re,collections
raw=open(sys.argv[1],'rb').read(); APP=int(sys.argv[2])
i=raw.find(b'TRACE:\n'); data=zlib.decompress(raw[i+7:]).decode('utf-8','replace')
pat=re.compile(r'^\s*\S+-(\d+)\s+\(\s*(\d+)\)\s+\[\d+\].*?\s(\d+\.\d+): tracing_mark_write: ([BE])\|(\d+)\|?(.*)$')
stacks=collections.defaultdict(list); secs=[]
for line in data.splitlines():
    m=pat.match(line)
    if not m: continue
    tid,pid,ts,kind,_,name=m.groups(); ts=float(ts)
    if int(pid)!=APP: continue
    if kind=='B': stacks[tid].append((name,ts,len(stacks[tid])))
    elif stacks[tid]:
        n,t0,depth=stacks[tid].pop(); secs.append((tid,n,t0,ts-t0,depth))
main=str(APP)
app=[s for s in secs if s[0]==main]
agg=collections.defaultdict(lambda:[0,0.0,0.0])
for tid,n,t0,d,depth in app:
    a=agg[n]; a[0]+=1; a[1]+=d; a[2]=max(a[2],d)
print("%-44s %5s %9s %8s"%("main-thread section","n","sum_ms","max_ms"))
for n,(c,sm,mx) in sorted(agg.items(), key=lambda x:-x[1][1])[:22]:
    print("%-44s %5d %9.1f %8.1f"%(n[:44],c,sm*1000,mx*1000))
frames=[s for s in app if s[1].startswith('Choreographer#doFrame')]
print("\nframes:",len(frames),"slow(>16.7ms):",sum(1 for f in frames if f[3]>0.0167), "sum slow ms: %.0f"%(sum(f[3] for f in frames if f[3]>0.0167)*1000))
def kids(tid,t0,d,depth):
    k=[s for s in app if s[2]>=t0 and s[2]+s[3]<=t0+d+1e-9 and s[4]==depth]
    k.sort(key=lambda s:-s[3]); return k
for f in sorted(frames,key=lambda f:-f[3])[:6]:
    tid,n,t0,d,depth=f
    k=kids(tid,t0,d,depth+1)
    print("frame %.1fms: "%(d*1000)+", ".join("%s=%.1f"%(x[1][:20],x[3]*1000) for x in k[:5]))
    for x in k[:2]:
        g=kids(tid,x[2],x[3],depth+2)
        print("   %s -> "%x[1][:14]+", ".join("%s=%.1f"%(y[1][:26],y[3]*1000) for y in g[:6]))
        for y in g[:1]:
            h=kids(tid,y[2],y[3],depth+3)
            print("      %s -> "%y[1][:14]+", ".join("%s=%.1f"%(z[1][:26],z[3]*1000) for z in h[:6]))
